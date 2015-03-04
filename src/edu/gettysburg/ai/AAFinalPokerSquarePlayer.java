package edu.gettysburg.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Stack;


public class AAFinalPokerSquarePlayer implements PokerSquaresPlayer{
	private final int SIZE = PokerSquares.SIZE;
	private static Random random = new Random();
	public static Stack<Integer> plays = new Stack<Integer>();
	public static ArrayList<int[]> empty = new ArrayList<int[]>();
	public static String[] rNames = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K"};
	public static String[] sNames = {"C", "D", "H", "S"};
	public static int trialsPerEmptySpace = 2500;  //100 give around 107 avg. for a 10000 games play
	public static long currentTime = System.currentTimeMillis();
	public static long startTime = System.currentTimeMillis();
	public static long millisRem;
	public static long timeCutOff = 500;
	public static Card[] allCard = new Card[rNames.length * sNames.length];
	public Card[][] cardGrid = new Card[SIZE][SIZE];  //Stores card on game grid
	public static int officialTracker = 0;  //Keep track of location in the allCard array between used cards and unknown cards
	public static boolean monteCarloFlushPriority = false;  //hold the current priority being used by monteCarlo
	public static boolean firstCard = true;

	@Override
	public void init() {
		plays.clear();
		Collections.shuffle(plays);
		plays = new Stack<Integer>();
		cardGrid = new Card[SIZE][SIZE];  //Stores card on game grid
		officialTracker = 0;
		currentTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		empty.clear();
		for (int row = 0; row < SIZE; row++){  //Initialize playGridRank to -1
			for (int col = 0; col < SIZE; col++)
				cardGrid[row][col] = null;
		}

		int i = 0;
		for (int rank = 0; rank < rNames.length; rank++) {
			for (int suit = 0; suit < sNames.length; suit++) {
				allCard[i] = new Card(rank, suit);
				i++;
			}
		}
	}

	public int[] algo (Card card, Card[][] grid, int operation, long millisRemaining) {
		int[] position = {0,0};
		int currentSpace = AATools.emptySpace(grid).size();
		if (AATools.possibleFour(grid, card)[0] != -1) {
			return AATools.possibleFour(grid, card);
		}

		if (firstCard == true && AATools.emptySpace(grid).size() == 25) {
			firstCard = false;
			return position;
		}

		if (currentSpace == 24) {
			return position = AAFullHousePokerSquarePlayer.fullHousePriority(card, grid, 3, millisRemaining);
		}

		if (currentSpace == 1) {
			return position = AATools.emptySpace(grid).get(0);
		}

		return position = AAMonteCarlo.MonteCarlo(cardGrid, card, 0, -1, -1, empty, millisRemaining, trialsPerEmptySpace);
		//		return position = AAFullHousePokerSquarePlayer.fullHousePriority(card, cardGrid, 3, millisRemaining);
	}

	@Override
	public int[] getPlay(Card card, long millisRemaining) {
		//int[] playPos = AAFlush.flushPriority(card, cardGrid, 3, millisRemaining); //position given in terms of a array.
		//int[] playPos = AAFullHouse.fullHousePriority(card, cardGrid, 3, millisRemaining);
		//int[] playPos = algo(card, cardGrid, 1, millisRemaining);
		int[] playPos = AAMonteCarlo.MonteCarlo(cardGrid, card, 0, -1, -1, empty, millisRemaining, trialsPerEmptySpace);
		//int[] playPos = AAFlushOnly.flushOnly(card, cardGrid, 3, millisRemaining);
		//int[] playPos = AAFullHouseOnly.rankOnly(card, cardGrid, 1, millisRemaining);
		System.out.println("Remaining: " + millisRemaining);
		//System.out.println(AATools.cardNames(card.getRank(), card.getSuit()));
		cardGrid[playPos[0]][playPos[1]] = card;
		allCard = AATools.movingCard(allCard, card, officialTracker);
		officialTracker++;
		return playPos;
	}

	public static void main(String[] args) {
		new PokerSquares(new AAFinalPokerSquarePlayer(), 60000L).play(); // play a single game

	}
	public static void start(){
		//new PokerSquares(new AAFinalPokerSquarePlayer(), 60000L).play();
	}
}
