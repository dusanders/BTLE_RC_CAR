/*	Author: Dustin Anderson
*			main.c
*			This file is intended to be the main program that will
*				control the MSP430G2553 of the BTLE RC Car.
*/


#include "io430.h"

#define BUFF_LEN 20

char *type;			//what are we returning?
char buffer[BUFF_LEN];		//buffer for recieve
//char *characteristicForward = "Indicate,002A,";
//char *characteristicBackwards = "Indicate,002C,";
//char *characteristicLeft = "Indicate,002E,";
//char *characteristicRight = "Indicate,0030,";
char *stateOn = "Connected";
char *aok = "AOK\r\n";


unsigned int forward;
unsigned int backwards;
unsigned int left;
unsigned int right;
char direction;
volatile int sendPointer = 0;
volatile int receivePointer = 0;
int moving;
int movingLeft;
int movingRight;

void delay(int timer);

int main(void)
{
  WDTCTL = WDTPW + WDTHOLD;                 // Stop WDT
  //MAKE SURE EVERYTHING IS OFF
  P1OUT = 0x00;
  P1DIR = 0x00;
  P2OUT = 0x00;
  P2DIR = 0x00;


  //RESET PROCEDURE FOR FULL SYSTEM RESET
  //THIS RESETS ALL SETTINGS IN RN4020
/*
  P1DIR |= BIT4;	//1.4 OUTPUT FOR SW_WAKE
  P1OUT |= BIT4;	//PUT SW_WAKE HIGH
  P1DIR |= BIT3;	//1.3 TO OUTPUT FOR CMD PIN
  P1OUT |= BIT3;	//PUT 1.3 HIGH FOR SYSTEM RESET
  P1DIR |= BIT6;	//1.6 TO OUTPUT FOR SYSTEM VDD
  P1OUT |= BIT6;	//1.6 ON FOR POWER
*/


  //SETUP OUR UART
  if (CALBC1_1MHZ==0xFF)					// If calibration constant erased
  {
    while(1);                               // do not load, trap CPU!!
  }
  DCOCTL = 0;                               // Select lowest DCOx and MODx settings
  P1SEL = BIT1 + BIT2 ;                     // P1.1 = RXD, P1.2=TXD
  P1SEL2 = BIT1 + BIT2 ;                    // P1.1 = RXD, P1.2=TXD

  //FOR USE WITH 9600 BAUD UART
  BCSCTL1 = CALBC1_1MHZ;
  BCSCTL3 = LFXT1S_2;
  DCOCTL = CALDCO_1MHZ;
  UCA0CTL1 |= UCSSEL_2;                     // SMCLK
  UCA0BR0 = 104;							//9600
  UCA0BR1 = 0;
/*
  //FOR USE WITH 115200 UART BAUD
 UCA0MCTL = UCBRS0;
 UCA0BR0 = 8;								//115200
 UCA0BR1 = 0;
 UCA0MCTL = UCBRS2 + UCBRS1;
*/

  UCA0CTL1 &= ~UCSWRST;                     // **Initialize USCI state machine**
  IE2 |= UCA0RXIE;                          // Enable USCI_A0 RX interrupt

__bis_SR_register(GIE);       // interrupts enabled
	//TURN ON SW_WAKE TO AVOID RESET <--BT Module
	P1DIR |= BIT4;
	P1OUT |= BIT4;
	//TURN ON DEVICE <--BT Module
	P1DIR |= BIT6;
	P1OUT |= BIT6;
	//SET OUTPUT FOR HW_WAKE <--BT Module
	P1DIR |= BIT7;
	P1OUT |= BIT7;
	moving = 0;
	movingLeft = 0;
	movingRight = 0;


  __bis_SR_register(LPM0_bits + GIE);       // Enter LPM0, interrupts enabled
}

//reserved for possible future use
void delay(int timer) {
  for (int i=0; i < timer; i++)
  {  }
}

//set PWM cycles
//void setTimer(int dutyCycle) {
void setTimer() {
  switch(direction)
  {
	  case 'A':				//forward
	 	//pin 2.2 set to TA1
	  	 moving = 1;
	  	 P2DIR |= BIT1;
		 P2SEL |= BIT1;
		 P2DIR |= BIT4;
		 P2SEL |= BIT4;
		 P2DIR &= ~BIT2;
		 P2SEL &= ~BIT2;
		 P2DIR &= ~BIT5;
		 P2SEL &= ~BIT5;
		// are we moving?
		if(forward == 0) {

		  moving = 0;
		  /*
		  movingLeft = 0;
		  movingRight = 0;
		  /*
		  P2DIR &= ~BIT1;
		  P2SEL &= ~BIT1;
		  P2DIR &= ~BIT4;
		  P2SEL &= ~BIT4;
		  P2DIR &= ~BIT2;
		  P2SEL &= ~BIT2;
		  P2DIR &= ~BIT5;
		  P2SEL &= ~BIT5;
		  */
		  TA1CTL = MC_0;
		  return;
		}
		//are we turning?
		if(movingLeft == 1)
		{
		  P2DIR &= ~BIT1;
		  P2SEL &= ~BIT1;
		}
		if(movingRight == 1)
		{
		  P2DIR &= ~BIT4;
		  P2SEL &= ~BIT4;
		}
		//set TA1 CCR0 to 100Hz
		TA1CCR0 = 10000;
		//get our duty cycle value (passed values are 1 - 9)
//		TA1CCR1 = dutyCycle*1000;	//2.1
		TA1CCR1 = forward*1000;
		TA1CCTL1 = OUTMOD_7;
//		TA1CCR2 = dutyCycle*1000;	//2.4
		TA1CCR2 = forward*1000;
		TA1CCTL2 = OUTMOD_7;
		TA1CTL = TASSEL_2 + MC_1 + TACLR;
		break;
	  case 'E':				//left
	  	//pin 2.1
		//let everyone know we are turning
	    movingLeft = 1;
		movingRight = 0;
		if(left == 0) {
		  movingLeft = 0;

		  P2DIR &= ~BIT1;
		  P2SEL &= ~BIT1;
		  P2DIR &= ~BIT4;
		  P2SEL &= ~BIT4;
		  P2DIR &= ~BIT2;
		  P2SEL &= ~BIT2;
		  P2DIR &= ~BIT5;
		  P2SEL &= ~BIT5;

		  TA1CTL = MC_0;
		  return;
		}
		//are we currently moving?
		if(moving == 1)
		{
		  P2DIR &= ~BIT1;
		  P2SEL &= ~BIT1;
		  TA1CCR1 = left*1000;
		  TA1CCTL2 = OUTMOD_7;
		  TA1CTL = TASSEL_2 + MC_1 + TACLR;
		  break;
		}
		//are we stopped?
		if(moving == 0)
		{
		  P2DIR |= BIT4;
		  P2SEL |= BIT4;
		  P2DIR |= BIT2;
		  P2SEL |= BIT2;
		  P2DIR &= ~BIT1;
		  P2SEL &= ~BIT1;
		  P2DIR &= ~BIT5;
		  P2SEL &= ~BIT5;
		  TA1CCR0 = 10000;
//		  TA1CCR1 = dutyCycle*1000;	//2.2
		  TA1CCR1 = 9800;
		  TA1CCTL1 = OUTMOD_7;
//		  TA1CCR2 = dutyCycle*1000;	//2.4
		  TA1CCR2 = 9800;
		  TA1CCTL2 = OUTMOD_7;
		  TA1CTL = TASSEL_2 +MC_1 + TACLR;
		  break;
		}
	  case 'C':             //backwards
	      moving = 1;
	  	  P2DIR |= BIT2;
		  P2SEL |= BIT2;
		  P2DIR |= BIT5;
		  P2SEL |= BIT5;
		  P2DIR &= ~BIT1;
		  P2SEL &= ~BIT1;
		  P2DIR &= ~BIT4;
		  P2SEL &= ~BIT4;
		if(backwards == 0) {

		  moving = 0;
		  /*
		  movingLeft = 0;
		  movingRight = 0;
		  /*
		  P2DIR &= ~BIT1;
		  P2SEL &= ~BIT1;
		  P2DIR &= ~BIT4;
		  P2SEL &= ~BIT4;
		  P2DIR &= ~BIT2;
		  P2SEL &= ~BIT2;
		  P2DIR &= ~BIT5;
		  P2SEL &= ~BIT5;
		  */
		  TA1CTL = MC_0;
		  return;
		}
		if(movingLeft == 1)
		{
		  P2DIR &= ~BIT2;
		  P2SEL &= ~BIT2;
		}
		if(movingRight == 1)
		{
		  P2DIR &= ~BIT5;
		  P2SEL &= ~BIT5;
		}
		//set TA1 CCR to 100Hz
		TA1CCR0 = 10000;
		//set our duty cycle
//		TA1CCR1 = dutyCycle*1000;	//2.2
		TA1CCR1 = backwards*1000;
		TA1CCTL1 = OUTMOD_7;
//		TA1CCR2 = dutyCycle*1000;	//2.5
		TA1CCR2 = backwards*1000;
		TA1CCTL2 = OUTMOD_7;
		TA1CTL = TASSEL_2 + MC_1 + TACLR;
		break;
	  case '0':				//right
	  	//pin 2.5 TA1.2
	    movingRight = 1;
		movingLeft = 0;
		if(right == 0) {
		  movingRight = 0;

		  P2DIR &= ~BIT1;
		  P2SEL &= ~BIT1;
		  P2DIR &= ~BIT4;
		  P2SEL &= ~BIT4;
		  P2DIR &= ~BIT2;
		  P2SEL &= ~BIT2;
		  P2DIR &= ~BIT5;
		  P2SEL &= ~BIT5;

		  TA1CTL = MC_0;
		  return;
		}

		if(moving == 1)
		{
		  P2DIR &= ~BIT5;
		  P2SEL &= ~BIT5;
		  TA1CCR2 = right*1000;
		  TA1CCTL2 = OUTMOD_7;
		  TA1CTL = TASSEL_2 + MC_1 + TACLR;
		  break;
		}

		if(moving == 0)
		{
		  P2DIR |= BIT5;
		  P2SEL |= BIT5;
		  P2DIR |= BIT1;
		  P2SEL |= BIT1;
		  P2DIR &= ~BIT2;
		  P2SEL &= ~BIT2;
		  P2DIR &= ~BIT4;
		  P2SEL &= ~BIT4;
		  TA1CCR0 = 10000;
//		  TA1CCR1 = dutyCycle*1000;
		  TA1CCR1 = 9800;
		  TA1CCTL1 = OUTMOD_7;
//		  TA1CCR2 = dutyCycle*1000;
		  TA1CCR2 = 9800;
		  TA1CCTL2 = OUTMOD_7;
		  TA1CTL = TASSEL_2 +MC_1 + TACLR;
		  break;
		}
  }
}

//check our connection status
void connectStatus() {
  int match;
  //examine received string
  for(int i=1; buffer[i] != '\r'; i++) {
	if(buffer[i] != stateOn[i]) {
	  match = 0;
	  break;
	}
	match = 1;
  }
  //connected - send command to start sensor
  if(match) {
	type = "chw,0032,0001\r";
	IE2 |= UCA0TXIE;
  }
}

//get our direction and duty cycle, set motors
void setMotor() {
	char c = buffer[12];
	switch(c) {
	  case 'A' :
	  	forward = (buffer[14] - '0') *10 + (buffer[15] - '0') ;
		direction = 'A';
//		setTimer(forward);
		setTimer();
		//send the ack
		type = "chw,0032,0001\r";
		IE2 |= UCA0TXIE;
		break;
	  case 'E' :
	  	left = (buffer[14] - '0') *10 + (buffer[15] - '0') ;
		direction = 'E';
//		setTimer(left);
		setTimer();
		//send the ack
		type = "chw,0032,0001\r";
		IE2 |= UCA0TXIE;
		break;
	  case 'C':
	  	backwards = (buffer[14] - '0') *10 + (buffer[15] - '0') ;
		direction = 'C';
//		setTimer(backwards);
		setTimer();
		//send the ack
		type = "chw,0032,0001\r";
		IE2 |= UCA0TXIE;
		break;
	  case '0':
	  	right = (buffer[14] - '0') *10 + (buffer[15] - '0') ;
		direction = '0';
//		setTimer(right);
		setTimer();
		//send the ack
		type = "chw,0032,0001\r";
		IE2 |= UCA0TXIE;
		break;
	}
 }

//clean out the buffer for received strings
void clearBuffer() {
  for(int i=0; i<BUFF_LEN; i++) {
	buffer[i] = '\0';
  }
}


//=============================================================================
//=============================ISR ROUTINES====================================
//=============================================================================

// UART RX ISR routine
#pragma vector=USCIAB0RX_VECTOR
__interrupt void USCI0RX_ISR(void)
{
  buffer[receivePointer++] = UCA0RXBUF;
  if(UCA0RXBUF == '\n') {
	receivePointer = 0;

	//shut up and wait while we get done setting up motors
__bic_SR_register(GIE);
	//decide what to do
	char c = buffer[0];
	switch(c) {

	  //if we get an 'AOK' --disregard it
	  case 'A':
	  	clearBuffer();
	  	break;

	  //did we get a 'Connected' or 'Connection End'?
	  case 'C':
		connectStatus();
		//clear the buffer out
		clearBuffer();
	    break;

	  //did we get an 'Indicate'?
	  case 'I':
	  	setMotor();
	  	break;
	}

//ok to talk again
__bis_SR_register(GIE);
  }
}


// UART TX ISR routine
#pragma vector = USCIAB0TX_VECTOR
__interrupt void USCI0TX_ISR(void)
{
  UCA0TXBUF = type[sendPointer++];
	if(type[sendPointer] == '\0')
	{
	  sendPointer = 0;
	  IE2 &= ~UCA0TXIE;
	}
}

