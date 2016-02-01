package edu.gettysburg.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
//import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;

/* The solitaire game "Poker Squares"
 * Author: Todd W. Neller

Notes: 

A Poker Squares grid is represented as a 5-by-5 array of Card objects.  A null indicates an empty position.
In the solitaire game of Poker Squares, a deck is initially shuffled.
Each turn, the player draws a card from the deck and places it in any empty cell of a 5-by-5 grid.  Once placed, cards may not be moved.
After the last cell is filled, each row and column are scored according to the American point system for Poker Squares hands:

100 - Royal Flush: A T-J-Q-K-A sequence all of the same suit. Example: TC, JC, QC, KC, AC.
75 - Straight Flush: Five cards in sequence all of the same suit. Example: AD, 2D, 3D, 4D, 5D.
50 - Four of a Kind: Four cards of the same rank. Example: 9C, 9D, 9H, 9S, 6H.
25 - Full House: Three cards of one rank with two cards of another rank. Example: 7S, 7C, 7D, 8H, 8S.
20 - Flush: Five cards all of the same suit. Example: AH, 2H, 3H, 5H, 8H.
15 - Straight: Five cards in sequence. Aces may be high or low but not both. (Straights do not wrap around.) Example: 8C, 9S, TH, JD, QC.
10 - Three of a Kind: Three cards of the same rank. Example: 2S, 2H, 2D, 5C, 7S.
5 - Two Pair: Two cards of one rank with two cards of another rank. Example: 3H, 3D, 4C, 4S, AC.
2 - One Pair: Two cards of one rank.  Example: 5D, 5H, TC, QS, AH.
(0 otherwise)

The player's total score is the sum of the scores for each of the 10 row and column hands.

Relevant Resources: http://tinyurl.com/pokersqrs

For our purposes, a player is considered better if it has a higher expected game score, i.e. has a higher score average over many games.

In our implementation, each turn a PokerSquaresPlayer will be passed (1) a Card object and (2) the number of milliseconds remaining in the game, 
and will return a length 2 integer array with the row and column the player placed the card.  In the event that the player makes an illegal 
play or "times out", i.e. runs out of time for play, the player loses with a final score of 0.

This file contains not only the code to run a simple demonstration game with a random player, but also has utility functions for scoring
that will be useful for coding good players.

 */

public class newPokerSquares {
	
	public static final int SIZE = 5; // square grid size
	public static final long GAME_MILLIS = 60000L; // 2013 contest maximum milliseconds per game

	private PokerSquaresPlayer player; // current player
	private long gameMillis; // maximum milliseconds for current game
	private boolean verbose = true; // whether or not to print move-by-move transcript of the game
	private Card[][] grid = new Card[SIZE][SIZE]; // current game grid
	private Random random = new Random(); // current game random number generator
	Stack<Card> deck;
	
	/**
	 * @param player Poker Squares player object
	 * @param gameMillis maximum milliseconds permitted for game
	 */
	public newPokerSquares(PokerSquaresPlayer player, long gameMillis, Stack<Card> inStack) {
		this.player = player;
		this.gameMillis = gameMillis;
		
		player.init();

		deck = new Stack<Card>();
		while(!inStack.isEmpty()){
			//System.out.println("CURRENT POSITION IN QUEUE: " + inStack.peek().getRank() + " " + inStack.peek().getSuit());
			// TODO
			deck.push(inStack.pop());
		}
		//System.out.println("THE DECK CONTAINS: " + deck);
		// TODO
		
		// shuffle deck
		/*deck = new Stack<Card>();
		for (Card card : Card.allCards)
			deck.push(card);
		Collections.shuffle(deck, random);*/
		
		// clear grid
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				grid[row][col] = null;
	}

	/**
	 * Play a game of Poker Squares with the given PokerSquaresPlayer and time limit, returning the game score.
	 * @return final game score
	 */
	public int nextMove() {
		
		
		// play game
		long millisRemaining = gameMillis;
		int cardsPlaced = 0;
		if (cardsPlaced < SIZE * SIZE) {
			Card card = deck.pop();
			//System.out.println("CURRENT CARD = " + card );
			// TODO
			long startTime = System.currentTimeMillis();
			int[] play = player.getPlay(card, millisRemaining);
			millisRemaining -= System.currentTimeMillis() - startTime;
			if (millisRemaining < 0) { // times out
				System.err.println("Player Out of Time");
				return 0;
			}
			if (play.length != 2 || play[0] < 0 || play[0] >= SIZE || play[1] < 0 || play[1] >= SIZE || grid[play[0]][play[1]] != null) { // illegal play
				System.err.printf("Illegal play: %s\n", Arrays.toString(play));
				return 0;
			}
			grid[play[0]][play[1]] = card;
			cardsPlaced++;
			if (verbose) {
				printGrid(grid);
				System.out.println();
				//TODO
			}
		}
		return getScore(grid);
	}
	
	/**
	 * Play a game of Poker Squares with the given PokerSquaresPlayer and time limit, returning the game score.
	 * @return final game score
	 */
	public int play(Scanner in) {
		player.init();

		// track remaining cards
		ArrayList<Card> remaining = new ArrayList<Card>();
		for (Card card : Card.allCards)
			remaining.add(card);
		Collections.shuffle(remaining, random);
		
		// clear grid
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				grid[row][col] = null;
		
		// play game
		long millisRemaining = gameMillis;
		int cardsPlaced = 0;
		while (cardsPlaced < SIZE * SIZE) {
			Card card = null;
			while (card == null) {
				System.out.println("Remaining cards: " + remaining);
				System.out.print("Card? ");
				String cardName = in.nextLine().trim().toUpperCase();
				card = Card.cardMap.get(cardName);
				if (card == null) {
					System.err.println("Error: Invalid card name");
					continue;
				}
				if (!remaining.contains(card)) {
					card = null;
					System.err.println("Error: Card already played");
					continue;
				}
				remaining.remove(card);
			}
			
			long startTime = System.currentTimeMillis();
			int[] play = player.getPlay(card, millisRemaining);
			millisRemaining -= System.currentTimeMillis() - startTime;
			if (millisRemaining < 0) { // times out
				System.err.println("Player Out of Time");
				return 0;
			}
			if (play.length != 2 || play[0] < 0 || play[0] >= SIZE || play[1] < 0 || play[1] >= SIZE || grid[play[0]][play[1]] != null) { // illegal play
				System.err.printf("Illegal play: %s\n", Arrays.toString(play));
				return 0;
			}
			grid[play[0]][play[1]] = card;
			cardsPlaced++;
			if (verbose) {
				printGrid(grid);
				System.out.println();
			}
		}
		return getScore(grid);
	}
	
	/**
	 * Play a sequence of games, collecting and reporting statistics.
	 * @param numGames number of games to play
	 * @param startSeed seed of first game. Successive games use successive seeds
	 * @param verbose whether or not to provide verbose output of game play
	 * @return integer array of game scores
	 */
	public int[] playSequence(int numGames, long startSeed, boolean verbose) {
		this.verbose = verbose;
		int[] scores = new int[numGames];
		double scoreMean = 0;
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int i = 0; i < numGames; i++) {
			random.setSeed(startSeed + i);
			int score = nextMove();
			scores[i] = score;
			scoreMean += score;
			if (scores[i] < min) min = scores[i];
			if (scores[i] > max) max = scores[i];
			System.out.println(score);
		}
		scoreMean /= numGames;
		double scoreStdDev = 0;
		for (int i = 0; i < numGames; i++) {
			double diff = scores[i] - scoreMean;
			scoreStdDev += diff * diff;
		}
		scoreStdDev = Math.sqrt(scoreStdDev / numGames);
		System.out.printf("Score Mean: %f, Standard Deviation: %f, Minimum: %d, Maximum: %d\n", scoreMean, scoreStdDev, min, max);
		return scores;
	}
	
	/**
	 * Print the current game grid and score.
	 * @param grid current game grid 
	 */
	public static void printGrid(Card[][] grid) {
		// get scores
		int[] handScores = getHandScores(grid);
		int totalScore = 0;
		for (int handScore : handScores)
			totalScore += handScore;
		
		// print grid
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) 
				System.out.printf(" %s ", grid[row][col] == null ? "--" : grid[row][col].toString());
			System.out.printf("%3d\n", handScores[row]);
		}
		for (int col = 0; col < SIZE; col++) 
			System.out.printf("%3d ", handScores[SIZE + col]);
		System.out.printf("%3d Total\n", totalScore);
	}
	
	/**
	 * Get the score of the given Card grid.
	 * @param grid Card grid
	 * @return score of given Card grid
	 */
	public static int getScore(Card[][] grid) {
		int[] handScores = getHandScores(grid);
		int totalScore = 0;
		for (int handScore : handScores)
			totalScore += handScore;
		return totalScore;
	}
	
	/**
	 * Get an int array with the individual hand scores of rows 0 through 4 followed by columns 0 through 4. 
	 * @param grid 2D Card array representing play grid
	 * @return an int array with the individual hand scores of rows 0 through 4 followed by columns 0 through 4. 
	 */
	public static int[] getHandScores(Card[][] grid) {
		int[] handScores = new int[2 * SIZE];
		for (int row = 0; row < SIZE; row++) {
			Card[] hand = new Card[SIZE];
			for (int col = 0; col < SIZE; col++)
				hand[col] = grid[row][col];
			handScores[row] = getHandScore(hand);
		}
		for (int col = 0; col < SIZE; col++) {
			Card[] hand = new Card[SIZE];
			for (int row = 0; row < SIZE; row++)
				hand[row] = grid[row][col];
			handScores[SIZE + col] = getHandScore(hand);
		}
		return handScores;
	}
	
	/**
	 * Get the score of the given Card hand.
	 * @param hand Card hand
	 * @return score of given Card hand.
	 */
	public static int getHandScore(Card[] hand) {
		// Compute counts
		int[] rankCounts = new int[Card.NUM_RANKS];
		int[] suitCounts = new int[Card.NUM_SUITS];
		for (Card card : hand)
			if (card != null) {
				rankCounts[card.getRank()]++;
				suitCounts[card.getSuit()]++;
			}
		
		// Compute count of rank counts
		int maxOfAKind = 0;
		int[] rankCountCounts = new int[hand.length + 1];
		for (int count : rankCounts) {
				rankCountCounts[count]++;
				if (count > maxOfAKind)
					maxOfAKind = count;
			}
			
		// Flush check
		boolean hasFlush = false;
		for (int i = 0; i < Card.NUM_SUITS; i++)
			if (suitCounts[i] != 0) {
				if (suitCounts[i] == hand.length)
					hasFlush = true;
				break;
			}
		
		// Straight check
		boolean hasStraight = false;
		boolean hasRoyal = false;
		int rank = 0;
		while (rank <= Card.NUM_RANKS - 5 && rankCounts[rank] == 0)
			rank++;
		hasStraight = (rank <= Card.NUM_RANKS - 5 && rankCounts[rank] == 1 && rankCounts[rank + 1] == 1 && rankCounts[rank + 2] == 1 && rankCounts[rank + 3] == 1 && rankCounts[rank + 4] == 1);
		if (rankCounts[0] == 1 && rankCounts[12] == 1 && rankCounts[11] == 1 && rankCounts[10] == 1 && rankCounts[9] == 1) 
			hasStraight = hasRoyal = true;
		
		// Return score
		if (hasFlush) {
			if (hasRoyal)
				return 100; // Royal Flush
			if (hasStraight)
				return 75; // Straight Flush
		}
		if (maxOfAKind == 4)
			return 50; // Four of a Kind
		if (rankCountCounts[3] == 1 && rankCountCounts[2] == 1)
			return 25; // Full House
		if (hasFlush)
			return 20; // Flush
		if (hasStraight)
			return 15; // Straight
		if (maxOfAKind == 3)
			return 10; // Three of a Kind
		if (rankCountCounts[2] == 2)
			return 5; // Two Pair
		if (rankCountCounts[2] == 1)
			return 2; // One Pair
		return 0; // Otherwise, score nothing.
	}
	
	/**
	 * Set the seed of the game pseudorandom number generator.
	 * @param seed pseudorandom number generator seed
	 */
	public void setSeed(long seed) {
		random.setSeed(seed);
	}
	
	/**
	 * Test the correctness of scoring code. 
	 */
	public static void scoreTest() {
		// Wikipedia example
		String[][] testGrid = {{"AH", "AD", "JS", "JC", "JH"},
				{"9H", "7D", "9S", "9C", "7H"},
				{"8H", "8D", "8S", "8C", "4D"},
				{"QH", "TD", "TS", "TC", "4C"},
				{"6H", "5D", "6S", "5C", "2D"}};

		//		// High scoring example
//		String[][] testGrid = {{"AH", "AD", "AS", "AC", "4H"},
//				{"KH", "KD", "KS", "KC", "8H"},
//				{"QH", "QD", "QS", "QC", "7H"},
//				{"JH", "JD", "JS", "JC", "6H"},
//				{"TH", "TD", "TS", "TC", "5H"}};
		
		Card[][] grid = new Card[SIZE][SIZE];
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				grid[row][col] = Card.cardMap.get(testGrid[row][col]);
		printGrid(grid);
	}
		
	public Card[][] getGrid() {
		return grid;
	}
	
	/**
	 * Demonstrate testing of a PokerSquaresPlayer.
	 * @param args
	 */
	public static void main(String[] args) {
		//new PokerSquares(new RandomPokerSquaresPlayer(), GAME_MILLIS).playSequence(10000, 0, false);
		//Score Mean: 14.423600, Standard Deviation: 7.684384, Minimum: 0, Maximum: 106
		//new PokerSquares(new SimplePokerSquaresPlayer(), GAME_MILLIS).playSequence(100, 0, false);
		//Score Mean: 48.990000, Standard Deviation: 24.702832, Minimum: 4, Maximum: 139
		//new PokerSquares(new FlushPokerSquaresPlayer(), GAME_MILLIS).playSequence(100, 0, false);
		//Score Mean: 83.800000, Standard Deviation: 18.231292, Minimum: 46, Maximum: 159
		//new PokerSquares(new SimpleMCPokerSquaresPlayer(), GAME_MILLIS).playSequence(100, 0, false);
		//Score Mean: 98.810000, Standard Deviation: 29.790836, Minimum: 50, Maximum: 207

		//new PokerSquares(new SimpleMCFlushPokerSquaresPlayer(), GAME_MILLIS).playSequence(100, 0, false);
		//Score Mean: 124.650000, Standard Deviation: 33.045234, Minimum: 54, Maximum: 226
//		new PokerSquares(new SimpleMCFlushPokerSquaresPlayer(), GAME_MILLIS).playSequence(10, 0, false);

		//new PokerSquares(new NARLPokerSquaresPlayer(), GAME_MILLIS).playSequence(100, 0, false);
		//Score Mean: 127.700000, Standard Deviation: 32.215059, Minimum: 68, Maximum: 229
		//new PokerSquares(new NARLPokerSquaresPlayer(), GAME_MILLIS).playSequence(10000, 0, false);
		//Score Mean: 123.334100, Standard Deviation: 31.678562, Minimum: 40, Maximum: 271
//		new PokerSquares(new NARLPokerSquaresPlayer(), GAME_MILLIS).play();
		
		//new PokerSquares(new SimpleMCPokerSquaresPlayer(new RandomPokerSquaresPlayer(), 10000), GAME_MILLIS).playSequence(100, 0, false);
		//Score Mean: 84.140000, Standard Deviation: 25.749571, Minimum: 40, Maximum: 157

		//new PokerSquares(new SimpleMCPokerSquaresPlayer(new FlushPokerSquaresPlayer(), 10000), GAME_MILLIS).playSequence(100, 0, false);
		//Score Mean: 112.200000, Standard Deviation: 33.923148, Minimum: 57, Maximum: 226
		//Why the difference between this and the SimpleMCFlushPokerSquaresPlayer???

		//new PokerSquares(new SimpleMCPokerSquaresPlayer(new NARLPokerSquaresPlayer(), 10), GAME_MILLIS).playSequence(100, 0, false);
		
//		new PokerSquares(new ExpectimaxNARLPokerSquaresPlayer(1), GAME_MILLIS).playSequence(100, 0, false);
		//Score Mean: 129.110000, Standard Deviation: 32.218285, Minimum: 76, Maximum: 244
		
//		PokerSquaresPlayer player = new ExpectimaxNARLPokerSquaresPlayer(1);
//		long timeStart = System.currentTimeMillis();
//		new PokerSquares(player, GAME_MILLIS).playSequence(1000, 0, false);
//		System.out.println("Time (ms): " + (System.currentTimeMillis() - timeStart));
		
//		Score Mean: 123.645000, Standard Deviation: 31.325500, Minimum: 53, Maximum: 244
		
//		new PokerSquares(new ExpectimaxNARLPokerSquaresPlayer2(100000), GAME_MILLIS).playSequence(100, 0, false);
//		Score Mean: 128.850000, Standard Deviation: 31.892750, Minimum: 76, Maximum: 244
		
//		new PokerSquares(new ExpectimaxNARLPokerSquaresPlayer(1), GAME_MILLIS).play();

// Final contest:

	    //new PokerSquares(new Player_WeiAndEleni(), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //Score Mean: 124.804000, Standard Deviation: 30.322295, Minimum: 55, Maximum: 240

	    //new PokerSquares(new Player_WeiAndMatt(), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //Score Mean: 111.475000, Standard Deviation: 27.897050, Minimum: 56, Maximum: 224

	    //new PokerSquares(new WeiAndMattCorrectVersion(), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //Score Mean: 119.146000, Standard Deviation: 29.415552, Minimum: 39, Maximum: 221

	    //new PokerSquares(new PlayerEx(), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //new PokerSquares(new PlayerEx(), GAME_MILLIS).playSequence(1, 967313920, false);

	    //new PokerSquares(new AAFinalPokerSquarePlayer(), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //Score Mean: 119.023000, Standard Deviation: 30.050565, Minimum: 29, Maximum: 246

	    //new PokerSquares(new IanKayPokerSquaresPlayer2(), GAME_MILLIS).playSequence(1, 100, false);
	    //Score Mean: 113.367000, Standard Deviation: 27.947134, Minimum: 58, Maximum: 232

	    //new PokerSquares(new Marcin_GreedyPlayer(), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //Score Mean: 114.809000, Standard Deviation: 28.454007, Minimum: 43, Maximum: 225

	    //new PokerSquares(new SimpleMCFlushPokerSquaresPlayer(), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //Score Mean: 120.849000, Standard Deviation: 32.669897, Minimum: 46, Maximum: 233

	    //new PokerSquares(new NARLPokerSquaresPlayer(), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //Score Mean: 124.644000, Standard Deviation: 30.743670, Minimum: 50, Maximum: 252

	    //new PokerSquares(new ExpectimaxNARLPokerSquaresPlayer(1), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //Score Mean: 125.010000, Standard Deviation: 32.307521, Minimum: 47, Maximum: 240

	    //new PokerSquares(new ExpectimaxNARLPokerSquaresPlayer2(100000), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //Score Mean: 125.177000, Standard Deviation: 32.075281, Minimum: 56, Maximum: 240

	    //new PokerSquares(new ExpectimaxNARLPokerSquaresPlayer3(21), GAME_MILLIS).playSequence(1000, 967313920, false);
	    //Score Mean: 124.815000, Standard Deviation: 30.622064, Minimum: 50, Maximum: 252

	}
	public static void start(){
		//new PokerSquares(new IanKayPokerSquaresPlayer2(), GAME_MILLIS).playSequence(1, 967313920, false);
		//new PokerSquares(new IanKayPokerSquaresPlayer2(), GAME_MILLIS).playSequence(1, 100, false);
	}
}
