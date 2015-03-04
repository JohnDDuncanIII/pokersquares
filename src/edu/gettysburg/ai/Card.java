package edu.gettysburg.ai;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * Card - Simple playing card class.
 * @author Todd W. Neller
 */
public class Card {

	public static final int NUM_RANKS = 13, NUM_SUITS = 4;
	public static Card[] allCards;
	public static String[] rankNames = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K"};
	public static String[] suitNames = {"C", "D", "H", "S"};
	public static HashMap<String, Card> cardMap = new HashMap<String, Card>();

	static {
		// create all card objects
		allCards = new Card[rankNames.length * suitNames.length];
		int i = 0;
		for (int suit = 0; suit < suitNames.length; suit++) 
			for (int rank = 0; rank < rankNames.length; rank++)
				allCards[i++] = new Card(rank, suit);
		// create mapping from String representations to Card objects
		for (Card card : allCards)
			cardMap.put(card.toString(), card);
	}

	private int rank, suit;

	/**
	 * Create a card with the given rank and suit.
	 * @param rank Card rank. Should be in range [0, NUM_RANKS - 1].
	 * @param suit Card suit. Should be in range [0, NUM_SUITS - 1].
	 */
	public Card(int rank, int suit) {
		this.rank = rank;
		this.suit = suit;
	}

	/**
	 * Get Card rank. Should be in range [0, NUM_RANKS - 1].
	 * @return Card rank. Should be in range [0, NUM_RANKS - 1].
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * Get Card suit. Should be in range [0, NUM_SUITS - 1].
	 * @return Card suit. Should be in range [0, NUM_SUITS - 1].
	 */
	public int getSuit() {
		return suit;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public java.lang.String toString() {
		return rankNames[rank] + suitNames[suit];
	}

	public boolean equals(Card other) {
		return this.rank == other.rank && this.suit == other.suit;		
	}

	public static Stack<Card> interpret (Stack<edu.gettysburg.pokersquares.Card> cards) {
		allCards = new Card[rankNames.length * suitNames.length];
		for(int i=0; i<(rankNames.length * suitNames.length); i++){
			
			edu.gettysburg.pokersquares.Card currentCard = cards.pop();
			
			int newRank = currentCard.rank().ordinal()+1;
			if(currentCard.rank().ordinal()==12)
				newRank = 0;
				
			System.out.println("CURRENT RANK AND SUIT TO INTERPRET =" + newRank + " " + currentCard.suit().ordinal());
			allCards[i] = new Card(newRank, currentCard.suit().ordinal());
			
			// create mapping from String representations to Card objects
			/*for (Card card : allCards)
				cardMap.put(card.toString(), card);*/
		}
		Stack<Card> toReturn = new Stack<Card>();
		for(int i=0; i<allCards.length; i++){
			toReturn.push(allCards[i]);
		}
		return toReturn;
	}

	/**
	 * Print all card objects.
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(Arrays.toString(allCards));
	}
}
