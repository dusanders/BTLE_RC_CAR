
#include "io430.h"
char *type;			//what are we returning?
char string[];		//string to send
int ADCResult;		//BS
volatile int pointer = 0;
int main(void)
{
  WDTCTL = WDTPW + WDTHOLD;                 // Stop WDT
  P1DIR |= BIT0;
  P1OUT &= ~BIT0;
  if (CALBC1_1MHZ==0xFF)					// If calibration constant erased
  {
    while(1);                               // do not load, trap CPU!!
  }
  DCOCTL = 0;                               // Select lowest DCOx and MODx settings
  BCSCTL1 = CALBC1_1MHZ;                    // Set DCO
  DCOCTL = CALDCO_1MHZ;
  P1SEL = BIT1 + BIT2 ;                     // P1.1 = RXD, P1.2=TXD
  P1SEL2 = BIT1 + BIT2 ;                    // P1.1 = RXD, P1.2=TXD
  UCA0CTL1 |= UCSSEL_2;                     // SMCLK
  UCA0BR0 = 104;                            // 1MHz 9600
  UCA0BR1 = 0;                              // 1MHz 9600
  UCA0MCTL = UCBRS0;                        // Modulation UCBRSx = 1
  UCA0CTL1 &= ~UCSWRST;                     // **Initialize USCI state machine**
  IE2 |= UCA0RXIE;                          // Enable USCI_A0 RX interrupt

  __bis_SR_register(LPM0_bits + GIE);       // Enter LPM0, interrupts enabled
}


void setP1xOUT(int index)
{
  if(index == 0x18)
	P1OUT |= BIT0;				// set to 1
  else if(index == 0x19)
	P1OUT &= ~BIT0;				// set to 0
  else if(index == 0x20)
	P1OUT |= BIT6;				// set to 1
  else if(index == 0x21)
	P1OUT &= ~BIT6;				// set to 0
}

void setP1xDIR(int index)
{
  if(index == 0x00)
	P1DIR &= ~BIT0;				//set to 0
  else if(index ==0x01)
	P1DIR &= ~BIT1;				//set to 0
  else if(index ==0x02)
	P1DIR &= ~BIT2;				//set to 0
  else if(index ==0x03)
	P1DIR &= ~BIT3;
  else if(index ==0x04)
	P1DIR &= ~BIT4;
  else if(index ==0x05)
	P1DIR &= ~BIT5;
  else if(index ==0x06)
	P1DIR &= ~BIT6;
  else if(index ==0x07)
	P1DIR &= ~BIT7;
  else if(index == 0x10)
	P1DIR |= BIT0;				//set to 1
  else if(index == 0x11)
	P1DIR |= BIT1;				//set to 1
  else if(index == 0x12)
	P1DIR |= BIT2;				//set to 1
  else if(index == 0x13)
	P1DIR |= BIT3;
  else if(index == 0x14)
	P1DIR |= BIT4;
  else if(index == 0x15)
	P1DIR |= BIT5;
  else if(index == 0x16)
	P1DIR |= BIT6;
  else if(index == 0x17)
	P1DIR |= BIT7;
}

void setADC10(int command)
{
  if(command == 0x22)
  {
	  ADC10CTL0 |= BIT4;			// Turn on ADC10
	  ADC10CTL0 |= ADC10IE;			// turn on Interrupts
	  ADC10CTL1 |= INCH_4;			// set input channell as P1.4
	  ADC10AE0	|=	BIT4;			// set analog input to P1.1
	  ADC10CTL0 |= ENC | ADC10SC;	// begin conversion
	//  IE2 &= ~UCA0TXIE;
  }
  else
  {
	ADC10CTL0 &= ~ADC10IE;			// turn off ADC10 Interrupts
  	ADC10CTL0 &= ~BIT4;				// turn off ADC10
//	IE2 &= ~UCA0TXIE;
  }
}//end setADC10()

void send_data(char *string)
{

}

//=============================================================================
//=============================ISR ROUTINES====================================
//=============================================================================


//  Echo back RXed character, confirm TX buffer is ready first
#pragma vector=USCIAB0RX_VECTOR
__interrupt void USCI0RX_ISR(void)
{
  P1OUT ^= BIT0;
// while (!(IFG2&UCA0TXIFG));                		// USCI_A0 TX buffer ready?
  P1OUT ^= BIT0;
//  if(UCA0RXBUF <= 0x17)								// did we get a P1.xDIR command?
//	setP1xDIR(UCA0RXBUF);							// launch it
//  else if(UCA0RXBUF > 0x17 && UCA0RXBUF <= 0x21)	// did we get a P1.xOUT command?
//	setP1xOUT(UCA0RXBUF);							// launch it
//  else if(UCA0RXBUF == 0x22 || UCA0RXBUF == 0x23)	// did we get a ADC10 command?
//	setADC10(UCA0RXBUF);
//  else												// send back UART
  {
//	type = "text ";
	string[0] = UCA0RXBUF;							// TX RX'd char
//	string[1] = 'T';
//	IE2 |= UCA0TXIE;								// enable TX interrupt to send data
  }
}


// UART TX ISR routine
#pragma vector = USCIAB0TX_VECTOR
__interrupt void USCI0TX_ISR(void)
{
//	UCA0TXBUF = type[pointer++];					// send pending string to TX
//	if(type[pointer] == '\0')								// string done?
//	{
//	  UCA0TXBUF = string[0];
//	  IE2 &= ~UCA0TXIE;								// shutdown TX interrupt
//	  pointer = 0;									// reset string pointer
//	  while(!(IFG2&UCA0TXIFG))
//	  ADC10CTL0 |= ENC | ADC10SC;					// begin conversion
	}
}


// ADC10 conversion ISR
#pragma vector = ADC10_VECTOR
__interrupt void ADC10INTERRUPT(void)
{
  type = "adc ";
  string[0] = ADC10MEM >> 3;					// shift result on 8 to send one UART signal
  IE2 |= UCA0TXIE;								// enable the TX interrupt to send data
}