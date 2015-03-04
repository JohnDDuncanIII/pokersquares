package edu.gettysburg.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
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
public class NARLPokerSquaresPlayer implements MCPokerSquaresPlayer {

	static private final int SIZE = PokerSquares.SIZE, NUM_RANKS = Card.NUM_RANKS, NUM_SUITS = Card.NUM_SUITS, NUM_CARDS = NUM_RANKS * NUM_SUITS;
	private Card[][] grid = new Card[SIZE][SIZE];
	private Card[] simDeck = Card.allCards.clone();
	private Random random = new Random(0);
	int numPlays = 0;
	double epsilon; // RL epsilon for e-greedy
	boolean isLearning = false;
	final double INITIAL_EXPECTED_VALUE = 100; // learning value = 100 (max possible hand score); use value = 0;
	HashMap<String, RLNode> absMap = new HashMap<String, RLNode>(); 
	String[][] history = new String[SIZE * SIZE][2 * SIZE];
	//	private double[] baseValues = new double[2 * SIZE];
	private double[] expValues = new double[SIZE * SIZE];
	//	private double[] cumProbs = new double[SIZE * SIZE];
	boolean verbose = false;
	int[] plays = new int[25];

	
	@SuppressWarnings("unchecked")
	public NARLPokerSquaresPlayer() {
		//System.out.println("IN NARL: " +  getClass().getClassLoader().getResource(".").getPath());
		/*
		PrintWriter writer;
		try {
			writer = new PrintWriter("the-file-name.txt", "UTF-8");
			writer.println("The first line");
			writer.println("The second line");
			writer.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		
		//File newfile = new File("");
		//System.out.println("SUPERCLASS FILE PATH: " + newfile.getAbsolutePath());

		// Read in file from location in Android system. 
				// To setup correctly for now, you need to push the file "narl.dat" from the root of the PokerSquares project directory into the given directory on the android file system
		File mapFile = new File("data/data/edu.gettysburg.pokersquares/files/narl.dat");
		if (mapFile.exists()) {
			try {
				FileInputStream fis = new FileInputStream(mapFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				absMap = (HashMap<String, RLNode>) ois.readObject();
				ois.close();
				
			} catch (Exception e) {
 				e.printStackTrace();
				System.exit(1);
			} 
		}
		else {
			System.out.println("Training NARL player...");
			trainGames(1000000, .1, .999975);
			System.out.println("Done.");
			try {
				FileOutputStream fos = new FileOutputStream(mapFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(absMap);
				oos.close();
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	class Position {
		public int row, col;

		public Position(int row, int col) {
			this.row = row;
			this.col = col;
		}
	}

	class RLNode implements Serializable {
		private static final long serialVersionUID = 2611554230124789566L;
		public String abstraction;
		public double expValue = INITIAL_EXPECTED_VALUE;
		public long numVisits = 0;

		public RLNode(String abstraction) {
			this.abstraction = abstraction;
		}

		public void update(double value) {
			expValue += (1.0 / ++numVisits) * (value - expValue);
		}

		public String toString() {
			return String.format("%s[expValue=%f,numVisits=%d]", abstraction, expValue, numVisits);
		}
		
		 private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			 out.writeObject(abstraction);
			 out.writeDouble(expValue);
			 out.writeLong(numVisits);
		 }
			 
		 private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
			 abstraction = (String) in.readObject();
			 expValue = in.readDouble();
			 numVisits = in.readLong();
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
		for (int row = 0; row < SIZE; row++) 
			history[numPlays][row] = absList[row];
		for (int col = 0; col < SIZE; col++)
			history[numPlays][SIZE + col] = absList[SIZE + col];

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
			// create set of empty positions
			ArrayList<Position> emptyPos = new ArrayList<Position>();
			for (int r = 0; r < SIZE; r++)
				for (int c = 0; c < SIZE; c++)
					if (grid[r][c] == null)
						emptyPos.add(new Position(r, c));

			//			// evaluate each row and column as if unchanged after placement
			//			for (int row = 0; row < SIZE; row++)
			//				baseValues[row] = getExpValue(String.format("%d:%s", numPlays + 1, history[numPlays][row]));
			//			for (int col = 0; col < SIZE; col++) 
			//				baseValues[SIZE + col] = getExpValue(String.format("%d:%s", numPlays + 1, history[numPlays][SIZE + col]));
			//			double baseTotal = 0;
			//			for (double value : baseValues)
			//				baseTotal += value;
			// If learning and with probability epsilon
			//			System.out.println(isLearning + " " + epsilon);
			if (isLearning && random.nextDouble() < epsilon) { 
				boolean isPlaced = false;
//				// try flush play
//				for (int row = 0; row < SIZE; row++)
//					if (grid[row][suit] == null) {
//						pos[0] = row;
//						pos[1] = suit;
//						isPlaced = true;
//						break;
//					}
//				if (!isPlaced)
//					// try last column
//					for (int row = 0; row < SIZE; row++)
//						if (grid[row][SIZE - 1] == null) {
//							pos[0] = row;
//							pos[1] = SIZE - 1;
//							isPlaced = true;
//							break;
//						}
				if (!isPlaced) {
					Position nextPos = emptyPos.get(random.nextInt(emptyPos.size())); // random play
					pos[0] = nextPos.row;
					pos[1] = nextPos.col;
				}
			}
			// Otherwise, choose placement according to max.
			else {
				int length = 0;
				int bestIndex = 0;
				// for each empty position, 
				for (Position nextPos : emptyPos) {
					// evaluate change in expected value after placement
					int nextRow = nextPos.row;
					int nextCol = nextPos.col;
					grid[nextRow][nextCol] = card;
					double nextTotal = getAbstractionScore(numPlays + 1);
					if (verbose)
						System.out.printf("[%d][%d] -> %f\n", nextRow, nextCol, nextTotal);
					grid[nextRow][nextCol] = null;
					// Track utilities and best so far.
					expValues[length] = nextTotal;
					if (expValues[length] > expValues[bestIndex])
						bestIndex = length;
					length++;
				}
				Position nextPos = emptyPos.get(bestIndex);
				pos[0] = nextPos.row;
				pos[1] = nextPos.col;
			}
		}

		// Make placement and return it.
		grid[pos[0]][pos[1]] = card;
		plays[numPlays] = pos[0] * 5 + pos[1];
		numPlays++;

		// If learning and last placement, compute grid hand score and update expectations for all prior row and column abstract configurations.
		if (isLearning && numPlays == SIZE * SIZE) {
			for (int row = 0; row < SIZE; row++) {
				Card[] hand = grid[row];
				int handScore = getHandScore(hand);
				for (int i = 0; i < SIZE * SIZE; i++)
					updateExpValue(String.format("%d:%s", i, history[i][row]), handScore);
			}
			for (int col = 0; col < SIZE; col++) {
				Card[] hand = new Card[SIZE];
				for (int row = 0; row < SIZE; row++)
					hand[row] = grid[row][col];
				int handScore = getHandScore(hand);
				for (int i = 0; i < SIZE * SIZE; i++)
					updateExpValue(String.format("%d:%s", i, history[i][SIZE + col]), handScore);
			}
		}
		return pos;
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

	public void trainGames(int numGames, double initialEpsilon, double epsilonDecay) {
		epsilon = initialEpsilon;
		long scoreTotal = 0;
		long tmpTotal = 0;
		int reportPeriod = 10000;
		for (int i = 0; i < numGames; i++) {
			int score = randomLearningGame();
			scoreTotal += score;
			tmpTotal += score;
			epsilon *= epsilonDecay;
			if (i % reportPeriod == reportPeriod - 1) {
				System.out.printf("%d iterations; Eps = %f; Average score (last %d): %f\n", i + 1, epsilon, reportPeriod, (double) tmpTotal / reportPeriod);
				tmpTotal = 0;
//				verbose = true;
//				isLearning = true;
//				new PokerSquares(this, 10000).play();
//				verbose = false;
//				isLearning = false;
			}
		}
		System.out.println("Average score: " + (double) scoreTotal / numGames);
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

	//	// Possible future elaboration: indication of remaining card for flush when there's a flush potential
	//	public static String getColAbstraction(int col, Card[][] grid) {
	//		StringBuilder sb = new StringBuilder("c");
	//		// Compute counts
	//		int numCards = 0;
	//		int[] rankCounts = new int[Card.NUM_RANKS];
	//		int[] suitCounts = new int[Card.NUM_SUITS];
	//		int minNonAceRank = Integer.MAX_VALUE;
	//		int maxNonAceRank = Integer.MIN_VALUE;
	//		boolean hasRepeatRank = false;
	//		for (int row = 0; row < SIZE; row++) {
	//			Card card = grid[row][col];
	//			if (card != null) {
	//				numCards++;
	//				int rank = card.getRank();
	//				int suit = card.getSuit();
	//				if (++rankCounts[rank] > 1)
	//					hasRepeatRank = true;
	//				suitCounts[suit]++;
	//				if (rank != 0) {
	//					minNonAceRank = Math.min(minNonAceRank, rank);
	//					maxNonAceRank = Math.max(maxNonAceRank, rank);
	//				}
	//			}
	//		}
	//		sb.append(numCards);
	//		if (numCards == 0)
	//			sb.append("f0/" + NUM_RANKS + "sr");
	//		else {
	//			// check flush potential
	//			int numSuits = 0;
	//			int flushSuit = 0;
	//			for (int s = 0; s < NUM_SUITS; s++)
	//				if (suitCounts[s] > 0) {
	//					numSuits++;
	//					flushSuit = s;
	//				}
	//			if (numSuits == 1) {
	//				sb.append("f");
	//				sb.append(numCards);
	//				sb.append("/");
	//				int dealtInFlushSuit = 0;
	//				for (int r = 0; r < SIZE; r++)
	//					for (int c = 0; c < SIZE; c++)
	//						if (grid[r][c] != null && grid[r][c].getSuit() == flushSuit)
	//							dealtInFlushSuit++;
	//				sb.append(NUM_RANKS - dealtInFlushSuit);
	//			}
	//			
	//			
	//			// check straight potential
	//			if (!hasRepeatRank && ((rankCounts[0] == 0 && maxNonAceRank != Integer.MIN_VALUE && (maxNonAceRank - minNonAceRank <= 4))
	//					|| (rankCounts[0] == 1 && (maxNonAceRank == Integer.MIN_VALUE || maxNonAceRank <= 4 || minNonAceRank >= 9)))) {
	//				sb.append("s");
	//				// check royal potential
	//				if (numSuits == 1 && minNonAceRank >= 9)
	//					sb.append("r");
	//			}
	//		}
	//		return sb.toString();
	//	}
	//	
	//	public static String getRowAbstraction(int row, Card[][] grid) {
	//		StringBuilder sb = new StringBuilder("r");
	//		
	//		// compute rank counts
	//		int[] rankCounts = new int[NUM_RANKS];
	//		int[] rowRankCounts = new int[NUM_RANKS];
	//		for (int r = 0; r < SIZE; r++)
	//			for (int c = 0; c < SIZE; c++) {
	//				Card card = grid[r][c];
	//				if (card != null) {
	//					int rank = card.getRank();
	//					rankCounts[rank]++;
	//					if (r == row)
	//						rowRankCounts[rank]++;
	//				}
	//			}
	//		int[] sortArray = new int[NUM_RANKS];
	//		for (int i = 0; i < NUM_RANKS; i++) {
	//			sortArray[i] = (NUM_SUITS + 1) * rowRankCounts[i] + (NUM_SUITS - rankCounts[i]); 
	//		}
	//		Arrays.sort(sortArray);
	//				
	//		// add non-zero row counts in decreasing order
	//		int i = sortArray.length - 1;
	//		while (i >= 0) {
	//			int rowRankCount = sortArray[i] / (NUM_SUITS + 1);
	//			int cardsLeftInRank = sortArray[i] % (NUM_SUITS + 1);
	//			sb.append(String.format("%d(%d)", rowRankCount, cardsLeftInRank));
	//			i--;
	//		}
	//		return sb.toString();
	//	}

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

	@Override
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

	@Override
	public void undoPlay() {
		numPlays--;
		int play = plays[numPlays];
		grid[play / SIZE][play % SIZE] = null;
	}

	public static void main(String[] args) {
		NARLPokerSquaresPlayer player = new NARLPokerSquaresPlayer();

		//		player.trainGames(1000000, .1, .999975);
		
		//		player.isLearning = true;
		//		player.epsilon = 1.0;
		
//				new PokerSquares(player, 10000).play();
//				new PokerSquares(player, 10000).play(new Scanner(System.in));
//				new PokerSquares(player, 10000).playSequence(2, 0, true);

				
//				new PokerSquares(new SimpleMCPokerSquaresPlayer(player, 10), 60000).play();

	}
}

/*
 * JOHN:
 * Training NARL player...
10000 iterations; Eps = 0.077880; Average score (last 10000): 96.193100
20000 iterations; Eps = 0.060653; Average score (last 10000): 107.386400
30000 iterations; Eps = 0.047236; Average score (last 10000): 110.960700
40000 iterations; Eps = 0.036787; Average score (last 10000): 114.079400
50000 iterations; Eps = 0.028650; Average score (last 10000): 115.822700
60000 iterations; Eps = 0.022313; Average score (last 10000): 117.266800
70000 iterations; Eps = 0.017377; Average score (last 10000): 118.923400
80000 iterations; Eps = 0.013533; Average score (last 10000): 119.895700
90000 iterations; Eps = 0.010540; Average score (last 10000): 121.050300
100000 iterations; Eps = 0.008208; Average score (last 10000): 121.974100
110000 iterations; Eps = 0.006393; Average score (last 10000): 122.029700
120000 iterations; Eps = 0.004979; Average score (last 10000): 122.489800
130000 iterations; Eps = 0.003877; Average score (last 10000): 122.563400
140000 iterations; Eps = 0.003020; Average score (last 10000): 123.043100
150000 iterations; Eps = 0.002352; Average score (last 10000): 123.078900
160000 iterations; Eps = 0.001831; Average score (last 10000): 122.755600
170000 iterations; Eps = 0.001426; Average score (last 10000): 123.221800
180000 iterations; Eps = 0.001111; Average score (last 10000): 123.093200
190000 iterations; Eps = 0.000865; Average score (last 10000): 123.264800
200000 iterations; Eps = 0.000674; Average score (last 10000): 123.771900
210000 iterations; Eps = 0.000525; Average score (last 10000): 123.699600
220000 iterations; Eps = 0.000409; Average score (last 10000): 123.401000
230000 iterations; Eps = 0.000318; Average score (last 10000): 123.690600
240000 iterations; Eps = 0.000248; Average score (last 10000): 123.702900
250000 iterations; Eps = 0.000193; Average score (last 10000): 123.877700
260000 iterations; Eps = 0.000150; Average score (last 10000): 123.621000
270000 iterations; Eps = 0.000117; Average score (last 10000): 123.336300
280000 iterations; Eps = 0.000091; Average score (last 10000): 122.983000
290000 iterations; Eps = 0.000071; Average score (last 10000): 123.555300
300000 iterations; Eps = 0.000055; Average score (last 10000): 124.118100
310000 iterations; Eps = 0.000043; Average score (last 10000): 123.918000
320000 iterations; Eps = 0.000034; Average score (last 10000): 123.719200
330000 iterations; Eps = 0.000026; Average score (last 10000): 123.549300
340000 iterations; Eps = 0.000020; Average score (last 10000): 123.262600
350000 iterations; Eps = 0.000016; Average score (last 10000): 124.234300
360000 iterations; Eps = 0.000012; Average score (last 10000): 123.562000
370000 iterations; Eps = 0.000010; Average score (last 10000): 123.877800
380000 iterations; Eps = 0.000007; Average score (last 10000): 123.576700
390000 iterations; Eps = 0.000006; Average score (last 10000): 123.990400
400000 iterations; Eps = 0.000005; Average score (last 10000): 124.211800
410000 iterations; Eps = 0.000004; Average score (last 10000): 123.783000
420000 iterations; Eps = 0.000003; Average score (last 10000): 123.606800
430000 iterations; Eps = 0.000002; Average score (last 10000): 123.470100
440000 iterations; Eps = 0.000002; Average score (last 10000): 123.354500
450000 iterations; Eps = 0.000001; Average score (last 10000): 123.853900
460000 iterations; Eps = 0.000001; Average score (last 10000): 123.292400
470000 iterations; Eps = 0.000001; Average score (last 10000): 123.057500
480000 iterations; Eps = 0.000001; Average score (last 10000): 123.511500
490000 iterations; Eps = 0.000000; Average score (last 10000): 123.584300
500000 iterations; Eps = 0.000000; Average score (last 10000): 124.127200
510000 iterations; Eps = 0.000000; Average score (last 10000): 123.631800
520000 iterations; Eps = 0.000000; Average score (last 10000): 123.102900
530000 iterations; Eps = 0.000000; Average score (last 10000): 123.860600
540000 iterations; Eps = 0.000000; Average score (last 10000): 124.267100
550000 iterations; Eps = 0.000000; Average score (last 10000): 123.677700
560000 iterations; Eps = 0.000000; Average score (last 10000): 124.325800
570000 iterations; Eps = 0.000000; Average score (last 10000): 123.131800
580000 iterations; Eps = 0.000000; Average score (last 10000): 123.769300
590000 iterations; Eps = 0.000000; Average score (last 10000): 123.230500
600000 iterations; Eps = 0.000000; Average score (last 10000): 123.391800
610000 iterations; Eps = 0.000000; Average score (last 10000): 123.356800
620000 iterations; Eps = 0.000000; Average score (last 10000): 123.337300
630000 iterations; Eps = 0.000000; Average score (last 10000): 124.285900
640000 iterations; Eps = 0.000000; Average score (last 10000): 123.208100
650000 iterations; Eps = 0.000000; Average score (last 10000): 123.564500
660000 iterations; Eps = 0.000000; Average score (last 10000): 124.236500
670000 iterations; Eps = 0.000000; Average score (last 10000): 122.944400
680000 iterations; Eps = 0.000000; Average score (last 10000): 123.295400
690000 iterations; Eps = 0.000000; Average score (last 10000): 123.752100
700000 iterations; Eps = 0.000000; Average score (last 10000): 123.518500
710000 iterations; Eps = 0.000000; Average score (last 10000): 123.878300
720000 iterations; Eps = 0.000000; Average score (last 10000): 123.939800
730000 iterations; Eps = 0.000000; Average score (last 10000): 123.336200
740000 iterations; Eps = 0.000000; Average score (last 10000): 123.647600
750000 iterations; Eps = 0.000000; Average score (last 10000): 123.215600
760000 iterations; Eps = 0.000000; Average score (last 10000): 123.484600
770000 iterations; Eps = 0.000000; Average score (last 10000): 123.760300
780000 iterations; Eps = 0.000000; Average score (last 10000): 123.348200
790000 iterations; Eps = 0.000000; Average score (last 10000): 123.429500
800000 iterations; Eps = 0.000000; Average score (last 10000): 123.179600
810000 iterations; Eps = 0.000000; Average score (last 10000): 123.310700
820000 iterations; Eps = 0.000000; Average score (last 10000): 124.263300
830000 iterations; Eps = 0.000000; Average score (last 10000): 123.384300
840000 iterations; Eps = 0.000000; Average score (last 10000): 123.883700
850000 iterations; Eps = 0.000000; Average score (last 10000): 123.232100
860000 iterations; Eps = 0.000000; Average score (last 10000): 123.514400
870000 iterations; Eps = 0.000000; Average score (last 10000): 123.672600
880000 iterations; Eps = 0.000000; Average score (last 10000): 123.187900
890000 iterations; Eps = 0.000000; Average score (last 10000): 123.252300
900000 iterations; Eps = 0.000000; Average score (last 10000): 123.458300
910000 iterations; Eps = 0.000000; Average score (last 10000): 122.961100
920000 iterations; Eps = 0.000000; Average score (last 10000): 123.618400
930000 iterations; Eps = 0.000000; Average score (last 10000): 123.323400
940000 iterations; Eps = 0.000000; Average score (last 10000): 123.260600
950000 iterations; Eps = 0.000000; Average score (last 10000): 123.521300
960000 iterations; Eps = 0.000000; Average score (last 10000): 122.887900
970000 iterations; Eps = 0.000000; Average score (last 10000): 123.174700
980000 iterations; Eps = 0.000000; Average score (last 10000): 123.211200
990000 iterations; Eps = 0.000000; Average score (last 10000): 123.446600
1000000 iterations; Eps = 0.000000; Average score (last 10000): 123.238600
Average score: 122.573055
Done.
 */


/* Training transcript:

Training NARL player...
10000 iterations; Eps = 0.077880; Average score (last 10000): 100.697300
20000 iterations; Eps = 0.060653; Average score (last 10000): 107.041800
30000 iterations; Eps = 0.047236; Average score (last 10000): 110.758200
40000 iterations; Eps = 0.036787; Average score (last 10000): 113.802600
50000 iterations; Eps = 0.028650; Average score (last 10000): 115.390300
60000 iterations; Eps = 0.022313; Average score (last 10000): 117.754600
70000 iterations; Eps = 0.017377; Average score (last 10000): 118.944700
80000 iterations; Eps = 0.013533; Average score (last 10000): 119.119300
90000 iterations; Eps = 0.010540; Average score (last 10000): 121.434000
100000 iterations; Eps = 0.008208; Average score (last 10000): 121.507300
110000 iterations; Eps = 0.006393; Average score (last 10000): 121.365000
120000 iterations; Eps = 0.004979; Average score (last 10000): 122.534600
130000 iterations; Eps = 0.003877; Average score (last 10000): 122.611800
140000 iterations; Eps = 0.003020; Average score (last 10000): 123.007400
150000 iterations; Eps = 0.002352; Average score (last 10000): 122.375700
160000 iterations; Eps = 0.001831; Average score (last 10000): 122.860900
170000 iterations; Eps = 0.001426; Average score (last 10000): 123.430800
180000 iterations; Eps = 0.001111; Average score (last 10000): 123.021100
190000 iterations; Eps = 0.000865; Average score (last 10000): 123.758300
200000 iterations; Eps = 0.000674; Average score (last 10000): 123.720700
210000 iterations; Eps = 0.000525; Average score (last 10000): 123.138100
220000 iterations; Eps = 0.000409; Average score (last 10000): 123.419300
230000 iterations; Eps = 0.000318; Average score (last 10000): 123.370300
240000 iterations; Eps = 0.000248; Average score (last 10000): 124.220600
250000 iterations; Eps = 0.000193; Average score (last 10000): 123.754400
260000 iterations; Eps = 0.000150; Average score (last 10000): 123.184700
270000 iterations; Eps = 0.000117; Average score (last 10000): 123.840000
280000 iterations; Eps = 0.000091; Average score (last 10000): 123.623900
290000 iterations; Eps = 0.000071; Average score (last 10000): 124.054700
300000 iterations; Eps = 0.000055; Average score (last 10000): 123.160000
310000 iterations; Eps = 0.000043; Average score (last 10000): 123.624200
320000 iterations; Eps = 0.000034; Average score (last 10000): 123.681100
330000 iterations; Eps = 0.000026; Average score (last 10000): 124.217700
340000 iterations; Eps = 0.000020; Average score (last 10000): 123.514900
350000 iterations; Eps = 0.000016; Average score (last 10000): 123.715600
360000 iterations; Eps = 0.000012; Average score (last 10000): 123.525000
370000 iterations; Eps = 0.000010; Average score (last 10000): 123.198000
380000 iterations; Eps = 0.000007; Average score (last 10000): 123.916100
390000 iterations; Eps = 0.000006; Average score (last 10000): 123.346300
400000 iterations; Eps = 0.000005; Average score (last 10000): 124.154200
410000 iterations; Eps = 0.000004; Average score (last 10000): 123.155800
420000 iterations; Eps = 0.000003; Average score (last 10000): 123.707800
430000 iterations; Eps = 0.000002; Average score (last 10000): 123.696800
440000 iterations; Eps = 0.000002; Average score (last 10000): 123.542000
450000 iterations; Eps = 0.000001; Average score (last 10000): 123.449500
460000 iterations; Eps = 0.000001; Average score (last 10000): 123.123400
470000 iterations; Eps = 0.000001; Average score (last 10000): 123.716100
480000 iterations; Eps = 0.000001; Average score (last 10000): 123.853300
490000 iterations; Eps = 0.000000; Average score (last 10000): 123.975900
500000 iterations; Eps = 0.000000; Average score (last 10000): 123.582400
510000 iterations; Eps = 0.000000; Average score (last 10000): 124.071600
520000 iterations; Eps = 0.000000; Average score (last 10000): 124.068600
530000 iterations; Eps = 0.000000; Average score (last 10000): 123.757200
540000 iterations; Eps = 0.000000; Average score (last 10000): 123.827500
550000 iterations; Eps = 0.000000; Average score (last 10000): 123.515900
560000 iterations; Eps = 0.000000; Average score (last 10000): 123.517400
570000 iterations; Eps = 0.000000; Average score (last 10000): 123.793400
580000 iterations; Eps = 0.000000; Average score (last 10000): 124.312900
590000 iterations; Eps = 0.000000; Average score (last 10000): 123.501800
600000 iterations; Eps = 0.000000; Average score (last 10000): 123.227900
610000 iterations; Eps = 0.000000; Average score (last 10000): 123.158000
620000 iterations; Eps = 0.000000; Average score (last 10000): 123.225100
630000 iterations; Eps = 0.000000; Average score (last 10000): 123.842300
640000 iterations; Eps = 0.000000; Average score (last 10000): 123.747100
650000 iterations; Eps = 0.000000; Average score (last 10000): 124.270700
660000 iterations; Eps = 0.000000; Average score (last 10000): 123.061700
670000 iterations; Eps = 0.000000; Average score (last 10000): 123.794400
680000 iterations; Eps = 0.000000; Average score (last 10000): 124.059800
690000 iterations; Eps = 0.000000; Average score (last 10000): 123.868800
700000 iterations; Eps = 0.000000; Average score (last 10000): 123.597100
710000 iterations; Eps = 0.000000; Average score (last 10000): 124.225500
720000 iterations; Eps = 0.000000; Average score (last 10000): 123.493100
730000 iterations; Eps = 0.000000; Average score (last 10000): 123.728200
740000 iterations; Eps = 0.000000; Average score (last 10000): 123.797900
750000 iterations; Eps = 0.000000; Average score (last 10000): 123.879000
760000 iterations; Eps = 0.000000; Average score (last 10000): 123.049400
770000 iterations; Eps = 0.000000; Average score (last 10000): 123.351700
780000 iterations; Eps = 0.000000; Average score (last 10000): 123.168300
790000 iterations; Eps = 0.000000; Average score (last 10000): 123.627800
800000 iterations; Eps = 0.000000; Average score (last 10000): 123.174200
810000 iterations; Eps = 0.000000; Average score (last 10000): 123.375700
820000 iterations; Eps = 0.000000; Average score (last 10000): 123.812200
830000 iterations; Eps = 0.000000; Average score (last 10000): 123.861500
840000 iterations; Eps = 0.000000; Average score (last 10000): 123.652100
850000 iterations; Eps = 0.000000; Average score (last 10000): 123.160400
860000 iterations; Eps = 0.000000; Average score (last 10000): 123.462200
870000 iterations; Eps = 0.000000; Average score (last 10000): 123.374300
880000 iterations; Eps = 0.000000; Average score (last 10000): 123.567000
890000 iterations; Eps = 0.000000; Average score (last 10000): 123.487700
900000 iterations; Eps = 0.000000; Average score (last 10000): 123.952400
910000 iterations; Eps = 0.000000; Average score (last 10000): 123.693600
920000 iterations; Eps = 0.000000; Average score (last 10000): 123.505500
930000 iterations; Eps = 0.000000; Average score (last 10000): 123.620500
940000 iterations; Eps = 0.000000; Average score (last 10000): 122.942900
950000 iterations; Eps = 0.000000; Average score (last 10000): 123.641800
960000 iterations; Eps = 0.000000; Average score (last 10000): 123.534700
970000 iterations; Eps = 0.000000; Average score (last 10000): 123.544200
980000 iterations; Eps = 0.000000; Average score (last 10000): 123.450600
990000 iterations; Eps = 0.000000; Average score (last 10000): 123.138700
1000000 iterations; Eps = 0.000000; Average score (last 10000): 123.937600
Average score: 122.640284
Done.
121
119
122
112
102
108
103
89
137
87
129
142
69
150
127
147
139
91
154
68
190
106
127
97
109
127
113
150
119
107
114
167
100
171
96
113
106
89
104
195
200
129
229
102
104
96
129
114
137
132
122
149
137
146
124
202
87
129
184
81
120
117
124
125
111
83
179
76
179
169
132
119
99
99
149
129
144
165
143
132
104
152
137
127
89
155
92
89
207
171
144
98
124
132
144
159
127
97
178
101
Score Mean: 127.700000, Standard Deviation: 32.215059, Minimum: 68, Maximum: 229


 */