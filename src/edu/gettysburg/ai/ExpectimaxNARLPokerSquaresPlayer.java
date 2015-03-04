package edu.gettysburg.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

/**
 * Naive Abstract Reinforcement Learning (NARL) Poker Squares Player
 * @author tneller
 *
 */
public class ExpectimaxNARLPokerSquaresPlayer extends NARLPokerSquaresPlayer implements PokerSquaresPlayer  {

	static private final int SIZE = PokerSquares.SIZE, NUM_RANKS = Card.NUM_RANKS, NUM_SUITS = Card.NUM_SUITS, NUM_CARDS = NUM_RANKS * NUM_SUITS;
	private Card[][] grid = new Card[SIZE][SIZE];
	private Card[] simDeck = Card.allCards.clone();
	private Random random = new Random(0);
	int numPlays = 0;
	double epsilon; // RL epsilon for e-greedy
	boolean isLearning = false;
	final double INITIAL_EXPECTED_VALUE = 0; // learning value = 100 (max possible hand score); use value = 0;
	HashMap<String, NARLPokerSquaresPlayer.RLNode> absMap = new HashMap<String, NARLPokerSquaresPlayer.RLNode>(); 
	//	private double[] baseValues = new double[2 * SIZE];
	private double[] expValues = new double[SIZE * SIZE];
	//	private double[] cumProbs = new double[SIZE * SIZE];
	boolean verbose = false;
	int[] plays = new int[25];
	int depthLimit = 1;


	@SuppressWarnings("unchecked")
	public ExpectimaxNARLPokerSquaresPlayer(int depthLimit) {
		this.depthLimit = depthLimit;
		File mapFile = new File("narl.dat");
		if (mapFile.exists()) {
			try {
				FileInputStream fis = new FileInputStream(mapFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				absMap = (HashMap<String, NARLPokerSquaresPlayer.RLNode>) ois.readObject();
				ois.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			} 
		}
		else {
			System.out.println("Cannot find file narl.dat");
			System.exit(2);
		}
	}

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
		int suit = card.getSuit();	

		// match simDeck to event
		int cardIndex = numPlays;
		while (!card.equals(simDeck[cardIndex]))
			cardIndex++;
		simDeck[cardIndex] = simDeck[numPlays];
		simDeck[numPlays] = card;

		// compute and record row, column abstractions
		String[] absList = getGridAbstraction(grid);
		if (verbose) System.out.println(Arrays.toString(absList));

		// compute play position
		int[] pos = new int[2];

		if (numPlays == 0) { // if first play, choose flush play position
			pos[0] = 0;
			pos[1] = suit;
		}
		else if (numPlays == SIZE * SIZE - 1) { // if last play, choose empty position
			for (int i = 0; i < SIZE * SIZE; i++) {
				int r = i / SIZE;
				int c = i % SIZE;
				if (grid[r][c] == null) {
					pos[0] = r;
					pos[1] = c;
					break;
				}
			}
		}
		else {
			Position nextPos = getExpectimaxPlay(card, 0);
			pos[0] = nextPos.row;
			pos[1] = nextPos.col;

		}

		// Make placement and return it.
		grid[pos[0]][pos[1]] = card;
		plays[numPlays] = pos[0] * 5 + pos[1];
		numPlays++;

		return pos;
	}

	
	public Position getExpectimaxPlay(Card card, int depth) { // depth-0 choice node
		// create set of empty positions
		ArrayList<Position> emptyPos = new ArrayList<Position>();
		for (int r = 0; r < SIZE; r++)
			for (int c = 0; c < SIZE; c++)
				if (grid[r][c] == null)
					emptyPos.add(new Position(r, c));
		// Choose placement according to max.
		int length = 0;
		double bestValue = Double.NEGATIVE_INFINITY;
		int bestIndex = 0;
		// for each empty position, 
		for (Position nextPos : emptyPos) {
			// evaluate change in expected value after placement
			int nextRow = nextPos.row;
			int nextCol = nextPos.col;
			grid[nextRow][nextCol] = card;
			numPlays++;
			double value = 0;
			if (numPlays == SIZE * SIZE)
				value = PokerSquares.getScore(grid);
			else if (depth == depthLimit)
				value = getAbstractionScore(numPlays);
			else
				value = getExpectimaxValue(depth);
			grid[nextRow][nextCol] = null;
			numPlays--;
			// Track utilities and best so far.
			if (value > bestValue) {
				bestValue = value;
				bestIndex = length;
			}
			length++;
		}
		return emptyPos.get(bestIndex);
	}
	
	public double getExpectimaxValue(int depth) { // chance node
		double averageValue = 0;
		int remainingCards = NUM_CARDS - numPlays;
		for (int i = numPlays; i < NUM_CARDS; i++) {
			// swap card with next deck card
			Card tmp = simDeck[numPlays];
			simDeck[numPlays] = simDeck[i];
			simDeck[i] = tmp;
			// get play value given each card possibly dealt next
			averageValue += getExpectimaxValue(simDeck[numPlays], depth + 1);
			// unswap cards
			simDeck[i] = simDeck[numPlays];
			simDeck[numPlays] = tmp;
		}
		averageValue /= remainingCards;
		return averageValue;
	}
	
	public double getExpectimaxValue(Card card, int depth) { // depth-1+ choice node
		// create set of empty positions
		ArrayList<Position> emptyPos = new ArrayList<Position>();
		for (int r = 0; r < SIZE; r++)
			for (int c = 0; c < SIZE; c++)
				if (grid[r][c] == null)
					emptyPos.add(new Position(r, c));
		// Choose placement according to max.
		int length = 0;
		double bestValue = Double.NEGATIVE_INFINITY;
		// for each empty position, 
		for (Position nextPos : emptyPos) {
			// evaluate change in expected value after placement
			int nextRow = nextPos.row;
			int nextCol = nextPos.col;
			grid[nextRow][nextCol] = card;
			numPlays++;
			if (numPlays == SIZE * SIZE)
				expValues[length] = PokerSquares.getScore(grid);
			else if (depth == depthLimit)
				expValues[length] = getAbstractionScore(numPlays);
			else
				expValues[length] = getExpectimaxValue(depth);
			grid[nextRow][nextCol] = null;
			numPlays--;
			// Track utilities and best so far.
			if (expValues[length] > bestValue)
				bestValue = expValues[length];
			length++;
		}
		return bestValue;
	}
	
	
	public double getExpValue(String abstraction) {
		RLNode node = absMap.get(abstraction);
		if (node == null) {
			node = new RLNode(abstraction);
			absMap.put(abstraction, node);
		}
		return node.expValue;
	}

	public double updateExpValue(String abstraction, double value) {
		RLNode node = absMap.get(abstraction);
		if (node == null) {
			node = new RLNode(abstraction);
			absMap.put(abstraction, node);
		}
		node.update(value);
		return node.expValue;
	}

	public int randomLearningGame() {
		isLearning = true;
		init();
		for (int i = 0; i < SIZE * SIZE; i++) {
			int j = random.nextInt(NUM_CARDS - i) + i;
			Card tmp = simDeck[i];
			simDeck[i] = simDeck[j];
			simDeck[j] = tmp;
			getPlay(simDeck[i], 0);
		}
		isLearning = false;
		return PokerSquares.getScore(grid);
	}

	public double getAbstractionScore(int numMoves) {
		String[] absList = getGridAbstraction(grid);
		double totalScore = 0;
		for (String abs : absList)
			totalScore += getExpValue(String.format("%d:%s", numMoves, abs));
		return totalScore;
	}

	public static String[] getGridAbstraction(Card[][] grid) {
		String[] absList = new String[2 * SIZE];

		// compute rank counts
		//		int numCards = 0;
		int[] rankCounts = new int[NUM_RANKS];
		int[] suitCounts = new int[NUM_SUITS];
		int[][] rowRankCounts = new int[SIZE][NUM_RANKS];
		int[][] colRankCounts = new int[SIZE][NUM_RANKS];
		int[][] colSuitCounts = new int[SIZE][NUM_SUITS];
		boolean[] colHasRepeatRank = new boolean[SIZE];
		int[] colMinNonAceRank = new int[SIZE];
		int[] colMaxNonAceRank = new int[SIZE];
		for (int i = 0; i < SIZE; i++) {
			colMinNonAceRank[i] = Integer.MAX_VALUE;
			colMaxNonAceRank[i] = Integer.MIN_VALUE;
		}
		int[] numRowCards = new int[SIZE];
		int[] numColCards = new int[SIZE];

		for (int r = 0; r < SIZE; r++)
			for (int c = 0; c < SIZE; c++) {
				Card card = grid[r][c];
				if (card != null) {
					//					numCards++;
					int rank = card.getRank();
					int suit = card.getSuit();
					rankCounts[rank]++;
					suitCounts[suit]++;
					rowRankCounts[r][rank]++;
					colRankCounts[c][rank]++;
					if (colRankCounts[c][rank] > 1)
						colHasRepeatRank[c] = true;
					colSuitCounts[c][suit]++;
					if (rank != 0) {
						if (rank < colMinNonAceRank[c])
							colMinNonAceRank[c] = rank;
						if (rank > colMaxNonAceRank[c])
							colMaxNonAceRank[c] = rank;
					}
					numRowCards[r]++;
					numColCards[c]++;
				}
			}

		// Create row abstraction strings
		for (int row = 0; row < SIZE; row++) {
			StringBuilder sb = new StringBuilder("r");
			int[] sortArray = new int[NUM_RANKS];
			for (int i = 0; i < NUM_RANKS; i++) {
				sortArray[i] = (NUM_SUITS + 1) * rowRankCounts[row][i] + (NUM_SUITS - rankCounts[i]); 
			}
			Arrays.sort(sortArray);

			// add non-zero row counts in decreasing order
			int i = sortArray.length - 1;
			while (i >= 0) {
				int rowRankCount = sortArray[i] / (NUM_SUITS + 1);
				if (rowRankCount == 0)
					break;
				int cardsLeftInRank = sortArray[i] % (NUM_SUITS + 1);
				if (numRowCards[row] == 5)
					sb.append(rowRankCount);
				else
					sb.append(String.format("%d(%d)", rowRankCount, cardsLeftInRank));
				i--;
			}
			absList[row] = sb.toString();
		}

		// Create column abstraction strings
		for (int col = 0; col < SIZE; col++) {
			StringBuilder sb = new StringBuilder("c");
			sb.append(numColCards[col]);
			if (numColCards[col] == 0)
				sb.append("f0(" + NUM_RANKS + ")sr");
			else {
				// check flush potential
				int numSuits = 0;
				int flushSuit = 0;
				for (int s = 0; s < NUM_SUITS; s++)
					if (colSuitCounts[col][s] > 0) {
						numSuits++;
						flushSuit = s;
					}
				if (numSuits == 1) {
					sb.append("f");
					sb.append(numColCards[col]);
					if (numColCards[col] != 5) {
						sb.append("(");
						int undealtInFlushSuit = NUM_RANKS - suitCounts[flushSuit];
						sb.append(undealtInFlushSuit);
						sb.append(")");
					}
				}

				// check straight potential
				if (!colHasRepeatRank[col] && ((colRankCounts[col][0] == 0 && colMaxNonAceRank[col] != Integer.MIN_VALUE && (colMaxNonAceRank[col] - colMinNonAceRank[col] <= 4))
						|| (rankCounts[0] == 1 && (colMaxNonAceRank[col] == Integer.MIN_VALUE || colMaxNonAceRank[col] <= 4 || colMinNonAceRank[col] >= 9)))) {
					sb.append("s");

					// check royal potential
					if (numSuits == 1 && colMinNonAceRank[col] >= 9)
						sb.append("r");
				}
			}
			absList[SIZE + col] = sb.toString();
		}
		return absList;
	}



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

	public void makePlay(Card card, int row, int col) {
		// match simDeck to event
		int cardIndex = numPlays;
		while (!card.equals(simDeck[cardIndex]))
			cardIndex++;
		simDeck[cardIndex] = simDeck[numPlays];
		simDeck[numPlays] = card;
		// make play
		grid[row][col] = card;
		plays[numPlays] = row * 5 + col;
		numPlays++;
	}

	public void undoPlay() {
		numPlays--;
		int play = plays[numPlays];
		grid[play / SIZE][play % SIZE] = null;
	}

	public static void main(String[] args) {
		ExpectimaxNARLPokerSquaresPlayer player = new ExpectimaxNARLPokerSquaresPlayer(1);

		//		player.trainGames(1000000, .1, .999975);

		//		player.isLearning = true;
		//		player.epsilon = 1.0;

		new PokerSquares(player, 10000).play();
		//				new PokerSquares(player, 10000).play(new Scanner(System.in));

	}
}

 