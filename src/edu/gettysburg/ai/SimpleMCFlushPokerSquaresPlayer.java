package edu.gettysburg.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Stack;

/*
SimpleMCFlushPokerSquaresPlayer - for each play position, perform n semi-random flush playouts and choose the position with the best average outcome.
 */

public class SimpleMCFlushPokerSquaresPlayer implements PokerSquaresPlayer {

	private final int SIZE = PokerSquares.SIZE, NUM_RANKS = Card.NUM_RANKS, NUM_SUITS = Card.NUM_SUITS, NUM_CARDS = NUM_RANKS * NUM_SUITS;
	private Card[][] grid = new Card[SIZE][SIZE];
	private int numPlays = 0;
	private int mcIter = 10000;
	private Card[] simDeck = Card.allCards.clone();
	private Random random = new Random();
	
	class Position {
		public int row, col;

		public Position(int row, int col) {
			this.row = row;
			this.col = col;
		}
	}
	
	@Override
	public void init() {
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				grid[row][col] = null;
		numPlays = 0;
	}

	@Override
	public int[] getPlay(Card card, long millisRemaining) {
		// match simDeck to event
		int cardIndex = numPlays;
		while (!card.equals(simDeck[cardIndex]))
			cardIndex++;
		simDeck[cardIndex] = simDeck[numPlays];
		simDeck[numPlays] = card;
		
		int[] pos = new int[2];
		if (numPlays == 0) {
			grid[0][card.getSuit()] = card;
			pos[1] = card.getSuit();
			numPlays++;
			return pos; // first play doesn't matter
		}

		// create set of empty positions
		ArrayList<Position> emptyPos = new ArrayList<Position>();
		for (int r = 0; r < SIZE; r++)
			for (int c = 0; c < SIZE; c++)
				if (grid[r][c] == null)
					emptyPos.add(new Position(r, c));
		
		int bestScoreSum = 0;
		
		for (Position playPos : emptyPos) { // for each possible play position
			grid[playPos.row][playPos.col] = card; // simulate play of the card
			int scoreSum = 0;
			for (int i = 0; i < mcIter; i++) { // for each Monte Carlo iteration
				// generate a possible card sequence playout beginning with given card
				for (int c1 = numPlays + 1; c1 < 25; c1++) { // shuffle cards through remaining game draws
					int c2 = c1 + random.nextInt(NUM_CARDS - c1 - 1);
					Card tmp = simDeck[c1];
					simDeck[c1] = simDeck[c2];
					simDeck[c2] = tmp;
				}
				// shuffle empty positions according to flush column
				ArrayList<Stack<Position>> colEmptyStacks = new ArrayList<Stack<Position>>();
				for (int s = 0; s < 5; s++)
					colEmptyStacks.add(new Stack<Position>());
				for (Position empty : emptyPos)
					if (empty != playPos)
						colEmptyStacks.get(empty.col).add(empty);
				for (int s = 0; s < 5; s++)
					Collections.shuffle(colEmptyStacks.get(s));
				// random flush playout
				int c = numPlays + 1;
				for (Position randPlayPos : emptyPos)
					if (randPlayPos != playPos) {
						Card nextCard = simDeck[c++];
						int playCol = nextCard.getSuit();
						if (colEmptyStacks.get(playCol).isEmpty()) {
							playCol = 4; // Try (1) suit col, (2) last col, (3) least occupied col
							if (colEmptyStacks.get(playCol).isEmpty()) {
								int maxEmptys = 0;
								for (int s = 0; s < 5; s++) {
									int size = colEmptyStacks.get(s).size();
									if (size > maxEmptys) {
										maxEmptys = size;
										playCol = s;
									}
								}
							}
						}
						Position simPlayPos = colEmptyStacks.get(playCol).pop();
						grid[simPlayPos.row][simPlayPos.col] = nextCard;
					}
				// accumulate score
				scoreSum += PokerSquares.getScore(grid);
			}
			// test to see if best play yet
			if (scoreSum >= bestScoreSum) {
				bestScoreSum = scoreSum;
				pos[0] = playPos.row;
				pos[1] = playPos.col;
			}
		}
		
		// clear empty positions
		for (Position playPos : emptyPos)  
			grid[playPos.row][playPos.col] = null;
		
		// make play and return
		grid[pos[0]][pos[1]] = card;
		numPlays++;
		return pos;
	}
	
	public static void main(String[] args) {
		//new PokerSquares(new SimpleMCFlushPokerSquaresPlayer(), 100000L).play(new Scanner(System.in));
		new PokerSquares(new SimpleMCFlushPokerSquaresPlayer(), 100000L).play();

	}
}
