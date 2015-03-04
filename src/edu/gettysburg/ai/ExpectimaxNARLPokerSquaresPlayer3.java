package edu.gettysburg.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Naive Abstract Reinforcement Learning (NARL) Poker Squares Player
 * @author tneller
 *
 */
public class ExpectimaxNARLPokerSquaresPlayer3 extends NARLPokerSquaresPlayer implements PokerSquaresPlayer  {

	static private final int SIZE = PokerSquares.SIZE, NUM_RANKS = Card.NUM_RANKS, NUM_SUITS = Card.NUM_SUITS, NUM_CARDS = NUM_RANKS * NUM_SUITS, NUM_POSITIONS = SIZE * SIZE;
	private Card[][] grid = new Card[SIZE][SIZE];
	private Card[] simDeck = Card.allCards.clone();
	//private Random random = new Random(0);
	int numPlays = 0;
	double epsilon; // RL epsilon for e-greedy
	boolean isLearning = false;
	final double INITIAL_EXPECTED_VALUE = 0; // learning value = 100 (max possible hand score); use value = 0;
	HashMap<String, NARLPokerSquaresPlayer.RLNode> absMap = new HashMap<String, NARLPokerSquaresPlayer.RLNode>(); 
	//private double[] baseValues = new double[2 * SIZE];
	//private double[] expValues = new double[SIZE * SIZE];
	//private double[] cumProbs = new double[SIZE * SIZE];
	boolean verbose = false;
	int[] plays = new int[25];
	int expectimaxDepth = 20;

	DancingLinkedListNode<Card> remainingCards;
	DancingLinkedListNode<Position> remainingPositions;
	TreeNode expectimaxTree;
	double expectimaxValue;

	class TreeNode {
		Position playPos;
		TreeNode[] afterNextCard;
	}

	@SuppressWarnings("unchecked")
	public ExpectimaxNARLPokerSquaresPlayer3(int expectimaxDepth) {
		//System.out.println("IN EXPECTIMAX: " +  getClass().getClassLoader().getResource(".").getPath());
		this.expectimaxDepth = expectimaxDepth;
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

		File newfile = new File("");
		System.out.println("CLASS FILE PATH: " + newfile.getAbsolutePath());
		
		if(newfile.canRead()){
			System.out.println("CHECK 1");
		}
		if(newfile.canWrite()){
			System.out.println("CHECK 2");
		}

		// Read in file from location in Android system. 
		// To setup correctly for now, you need to push the file "narl.dat" from the root of the PokerSquares project directory into the given directory on the android file system
		File mapFile = new File("data/data/edu.gettysburg.pokersquares/files/narl.dat");
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
		else if (numPlays == NUM_POSITIONS - 1) { // if last play, choose empty position
			for (int i = 0; i < NUM_POSITIONS; i++) {
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
			if (numPlays >= expectimaxDepth) {
				//				if (numPlays == expectimaxDepth) System.out.println("Building Expectimax Tree");
				Position nextPos = (numPlays == expectimaxDepth) ? (expectimaxTree = computeExpectimaxTree(card)).playPos : getExpectimaxPlay(card);
				pos[0] = nextPos.row;
				pos[1] = nextPos.col;
			}	
			else {
				double bestScore = -1;
				numPlays++;
				for (int i = 0; i < NUM_POSITIONS; i++) {
					int r = i / SIZE;
					int c = i % SIZE;
					if (grid[r][c] != null)
						continue;
					grid[r][c] = card;
					double score = getAbstractionScore(numPlays);
					if (score > bestScore) {
						bestScore = score;
						pos[0] = r;
						pos[1] = c;
					}
					grid[r][c] = null;
				}
				numPlays--;
			}
		}

		// Make placement and return it.
		grid[pos[0]][pos[1]] = card;
		plays[numPlays] = pos[0] * 5 + pos[1];
		numPlays++;

		return pos;
	}

	private TreeNode computeExpectimaxTree(Card card) {
		remainingCards = new DancingLinkedListNode<Card>();
		for (int i = numPlays + 1; i < NUM_CARDS; i++)
			remainingCards.insertNext(simDeck[i]);
		remainingPositions = new DancingLinkedListNode<Position>();
		for (int i = 0; i < NUM_POSITIONS; i++) {
			int r = i / SIZE;
			int c = i % SIZE;
			if (grid[r][c] == null)
				remainingPositions.insertNext(new Position(r, c));
		}
		return computeExpectimaxTreeHelper(card);
	}

	private TreeNode computeExpectimaxTreeHelper(Card card) {
		TreeNode newNode = new TreeNode();
		DancingLinkedListNode<Position> currentPosNode = remainingPositions.next;
		double bestValue = Double.NEGATIVE_INFINITY;
		while (currentPosNode != remainingPositions) {
			Position emptyPos = currentPosNode.data;
			currentPosNode.removeSelf();
			grid[emptyPos.row][emptyPos.col] = card;
			numPlays++;
			double value = Double.NEGATIVE_INFINITY;
			TreeNode[] afterNextCard = null;
			if (numPlays == NUM_POSITIONS)
				value = PokerSquares.getScore(grid);
			else {
				//				System.out.println(card);
				//				System.out.printf("After %d plays, %d cards remain.\n", numPlays, NUM_CARDS - numPlays);
				afterNextCard = new TreeNode[NUM_CARDS - numPlays];
				double expScoreSum = 0;
				DancingLinkedListNode<Card> currentCardNode = remainingCards.next;
				int index = 0;
				while (currentCardNode != remainingCards) {
					Card nextCard = currentCardNode.data;
					//					System.out.println(index + " " + nextCard);
					currentCardNode.removeSelf();
					afterNextCard[index++] = computeExpectimaxTreeHelper(nextCard);
					expScoreSum += expectimaxValue;
					currentCardNode.reinsertSelf();
					currentCardNode = currentCardNode.next;
				}
				value = expScoreSum / (NUM_CARDS - numPlays);
			}
			if (value > bestValue) {
				bestValue = value;
				newNode.playPos = emptyPos;
				newNode.afterNextCard = afterNextCard;
			}
			numPlays--;
			grid[emptyPos.row][emptyPos.col] = null;
			currentPosNode.reinsertSelf();
			currentPosNode = currentPosNode.next;
		}
		expectimaxValue = bestValue;
		return newNode;
	}

	private Position getExpectimaxPlay(Card card) {
		DancingLinkedListNode<Card> currentCardNode = remainingCards.next;
		int index = 0;
		while (currentCardNode.data != card) {
			currentCardNode = currentCardNode.next;
			index++;
		}
		currentCardNode.removeSelf();
		expectimaxTree = expectimaxTree.afterNextCard[index];
		DancingLinkedListNode<Position> currentPositionNode = remainingPositions.next;
		while (currentPositionNode.data != expectimaxTree.playPos) 
			currentPositionNode = currentPositionNode.next;
		currentPositionNode.removeSelf();
		Position playPos = expectimaxTree.playPos;
		return playPos;
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

	public static void main(String[] args) {
		ExpectimaxNARLPokerSquaresPlayer3 player = new ExpectimaxNARLPokerSquaresPlayer3(21);

		//		player.trainGames(1000000, .1, .999975);

		//		player.isLearning = true;
		//		player.epsilon = 1.0;

		new PokerSquares(player, 60000).play();
		//				new PokerSquares(player, 10000).play(new Scanner(System.in));

	}

	public static void start(){
		ExpectimaxNARLPokerSquaresPlayer3 player = new ExpectimaxNARLPokerSquaresPlayer3(21);
		new PokerSquares(player, 60000).play();
	}
}

