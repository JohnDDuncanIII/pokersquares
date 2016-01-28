package edu.gettysburg.pokersquares;

import java.io.Serializable;
import java.util.List;
import java.util.Stack;

public class Card implements Comparable<Card>, Serializable {
	
	private static final long serialVersionUID = 8219857561355671167L;

	public enum Rank { DEUCE, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE }
	public enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }

	private final Rank rank;
	private final Suit suit;
	private Card(Rank rank, Suit suit) {
		this.rank = rank;
		this.suit = suit;
	}

	public Rank rank() 		  { return rank; }
	public Suit suit() 		  { return suit; }
	public String toString() { return rank + "" + suit; }

	private static final Stack<Card> protoDeck = new Stack<Card>();

	/* //Initialize prototype deck
    static {

    	for (Suit suit : Suit.values())
            for (Rank rank : Rank.values())
                protoDeck.push(new Card(rank, suit));

    }*/

	public static Stack<Card> initialize() {
		Stack<Card> protoDeck = new Stack<Card>();
		for (Suit suit : Suit.values())
			for (Rank rank : Rank.values())
				protoDeck.push(new Card(rank, suit));

		return protoDeck;
	}


	public static Stack<Card> getDeck() {
		return protoDeck; //Return copy of prototype deck
	}

	/**
	 * Check all of the various Poker Hand types
	 */

	public static boolean isRoyalFlush(List<Card> ar) {
		Card.Rank val1 = ar.get(0).rank(); 
		Card.Rank val2 = ar.get(1).rank(); 
		Card.Rank val3 = ar.get(2).rank(); 
		Card.Rank val4 = ar.get(3).rank(); 
		Card.Rank val5 = ar.get(4).rank();

		if(isFlush(ar) && 
				val1.name()=="TEN" && 
				val2.name()=="JACK" && 
				val3.name()=="QUEEN" && 
				val4.name()=="KING" && 
				val5.name() == "ACE") {
			return true;
		}
		return false;
	}

	public static boolean isStraightFlush(List<Card> ar) {

		if(isStraight(ar) && isFlush(ar)) {
			return true;
		}
		return false;
	}

	public static boolean isFourOfAKind (List<Card> ar){
		for (int i = 0; i < ar.size()-3; i++) {
			Card.Rank val1 = ar.get(i).rank(); 
			Card.Rank val2 = ar.get(i+1).rank(); 
			Card.Rank val3 = ar.get(i+2).rank(); 
			Card.Rank val4 = ar.get(i+3).rank();

			if(val1 == val2 && val2 == val3 && val3 == val4) {
				return true;
			}
		}
		return false;
	}

	public static boolean isFullHouse(List<Card> ar) {

		Card.Rank val1 = ar.get(0).rank(); 
		Card.Rank val2 = ar.get(1).rank(); 
		Card.Rank val3 = ar.get(2).rank(); 
		Card.Rank val4 = ar.get(3).rank(); 
		Card.Rank val5 = ar.get(4).rank();

		if(((val1 == val2 && val2 == val3) && val4 == val5) || 
				(val1 == val2 && (val3 == val4 && val4 == val5))) {
			return true;
		}
		return false;
	}

	public static boolean isFlush(List<Card> ar) {
		Card.Suit val1 = ar.get(0).suit(); 
		Card.Suit val2 = ar.get(1).suit(); 
		Card.Suit val3 = ar.get(2).suit(); 
		Card.Suit val4 = ar.get(3).suit(); 
		Card.Suit val5 = ar.get(4).suit();

		if(val1 == val2 && val2 == val3 && val3 == val4 && val4 == val5){
			return true;
		}
		return false;
	}

	public static boolean isStraight(List<Card> ar) {
		int val1  = ar.get(0).rank().ordinal();
		int val2  = ar.get(1).rank().ordinal();
		int val3  = ar.get(2).rank().ordinal();
		int val4  = ar.get(3).rank().ordinal();
		int val5  = ar.get(4).rank().ordinal();

		if(val1 == val2-1 && val2-1 == val3-2 && val3-2 == val4-3 && val4-3 == val5-4){
			return true;
		}
		return false;
	}

	public static boolean hasThreeOfAKind(List<Card> ar) {
		for (int i = 0; i < ar.size()-2; i++) {
			Card.Rank firstValue  = ar.get(i).rank();
			Card.Rank secondValue = ar.get(i+1).rank();
			Card.Rank thirdValue  = ar.get(i+2).rank();
			if (firstValue == secondValue && secondValue == thirdValue) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Some concepts here taken from: http://www.mathcs.emory.edu/~cheung/Courses/170/Syllabus/10/pokerCheck.html
	 */
	public static boolean hasTwoPair(List<Card> ar, int size) {
		boolean a1 = false, a2 = false, a3 = false;

		Card.Rank[] tmp;

		if(size == 4){
			tmp = new Card.Rank[] {
					ar.get(0).rank(),
					ar.get(1).rank(),
					ar.get(2).rank(),
					ar.get(3).rank()};

			/* --------------------------------
	        Checking: a a  b b x
		 -------------------------------- */                       
			a1 = (tmp[0] == tmp[1] &&
					tmp[2] == tmp[3]) ;
		} else {  

			tmp = new Card.Rank[] {
					ar.get(0).rank(),
					ar.get(1).rank(),
					ar.get(2).rank(),
					ar.get(3).rank(), 
					ar.get(4).rank() };
			
			/* --------------------------------
	        Checking: a a  b b x
		 -------------------------------- */    
			a1 = (tmp[0] == tmp[1] &&
					tmp[2] == tmp[3]) ;

			/* ------------------------------
	        Checking: x a a  b b
		 ------------------------------ */
			a3 = (tmp[1] == tmp[2] &&
					tmp[3] == tmp[4]) ;
			/* ------------------------------
	        Checking: a a x  b b
		 ------------------------------ */
			a2 = (tmp[0] == tmp[1] &&
					tmp[3] == tmp[4]) ;
		}

		return( a1 || a2 || a3 );
	}

	public static boolean hasPair(List<Card> ar) {
		for (int i = 0; i < ar.size()-1; i++) {
			Card.Rank firstValue  = ar.get(i).rank();
			Card.Rank secondValue = ar.get(i+1).rank();

			if (firstValue == secondValue) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int compareTo(Card another) {
		return rank.compareTo(another.rank());
	}
}
