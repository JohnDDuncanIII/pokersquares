package edu.gettysburg.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Stack;

public class AAFullHousePokerSquarePlayer implements PokerSquaresPlayer {
	private final static int SIZE = PokerSquares.SIZE;
	public static String[] rNames = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K"};
	public static String[] sNames = {"C", "D", "H", "S"};
	public static int trialsPerEmptySpace = AAFinalPokerSquarePlayer.trialsPerEmptySpace;  //100 give around 107 avg. for a 10000 games play
	public static Card[][] cardGrid = new Card[SIZE][SIZE];  //Stores card on game grid
	public static Card[] allCard = new Card[rNames.length * sNames.length];
	public Stack<Integer> plays = new Stack<Integer>();
	public static int officialTracker = 0;  //Keep track of location in the allCard array between used cards and unknown cards
	static Random random = new Random();
	public static ArrayList<int[]> empty = new ArrayList<int[]>();
	public static long currentTime = System.currentTimeMillis();
	public static long startTime = System.currentTimeMillis();
	public static long millisRem;
	public static long timeCutOff = 500;
	//Annealer
	public Card[][] annealerNowGrid = new Card[SIZE][SIZE];
	public Card[][] annealerPreviousGrid = new Card[SIZE][SIZE];
	private State state;
	private double energy;
	private Card[][] minState;
	private double minEnergy;
	private double initialTemperature = 450, temperatureDecay = .99999;  //initialize allowed upHillStep
	//end annealer
	public static boolean useFlushPriority = false;  //hold the current priority being run
	public static boolean monteCarloFlushPriority = false;  //hold the current priority being used by monteCarlo
	public static boolean allowSwitchPriority = false;  //Control if switching priority is allowed is allowed
	public static boolean allowMonteCarlo = false;  //control if each game can use monteCarlo or not
	public static boolean allowAnnealer = false;  //control if each game can use annealer or not
	public static boolean MonteCarloOverAnnealer = true;  //switch between monteCarlo and annealer
	//default
	//	public static boolean[] switchLocation = {false, false, false, false, false,
	//		false, false, false, false, false, false, false, false, false};  //control where the flush switching appears //Currently have 13 locations
	//	public static boolean[] useMethod = {false, false, false, false, false,
	//		false, false, false, false, false, false, false, false, false};  //control which combination of method to use for switching
	//	public static int[] switchMethod = {2, 3, 1, 2};  //hold the order to check for switching
	//default
	//	public static boolean[] switchLocation = {false, false, false, false, false,
	//		true, false, false, false, false, true, true, false, true};  //control where the flush switching appears //Currently have 13 locations
	//	public static boolean[] useMethod = {true, true, true, true, true,
	//		false, false, false, false, false, false, false, false, false};  //control which combination of method to use for switching
	//	public static int[] switchMethod = {2, 3, 1, 2};  //hold the order to check for switching
	//0011100000 2312 HighAvg: 106.28905  falsetrue  falsefalse  falsefalse  falsefalse  falsefalse  
	//truefalse  falsefalse  falsefalse  falsefalse  falsefalse  truefalse  truefalse  falsefalse  truefalse 

	public String cardNames(int rank, int suit){ //convert getRank, getSuit into string.
		String cardName = rNames[rank] + sNames[suit];
		return cardName;
	}

	@Override
	public void init() {  //initialize the program
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

	//Start annealer methods
	public boolean positionConflict(int[] thisPosition, ArrayList<int[]> checkList) {
		for (int[] i : checkList)
			if (thisPosition[0] == i[0] && thisPosition[1] == i[1])
				return true;

		return false;
	}

	public void annealerStep(ArrayList<int[]> solidPosition) {
		for (int row = 0; row < SIZE; row++)  //Start copy the previous grid incase of backtrack
			for (int col = 0; col < SIZE; col++)
				annealerPreviousGrid[row][col] = annealerNowGrid[row][col];

		int[] position1 = {random.nextInt(SIZE), random.nextInt(SIZE)};
		int[] position2 = {random.nextInt(SIZE), random.nextInt(SIZE)};
		while (positionConflict(position1, solidPosition)) {
			int[] tmp = {random.nextInt(SIZE), random.nextInt(SIZE)};
			position1 = tmp;
		}
		while (positionConflict(position2, solidPosition)) {
			int[] tmp = {random.nextInt(SIZE), random.nextInt(SIZE)};
			position2 = tmp;
		}
		//		PokerSquares.printGrid(annealerNowGrid);
		//Switch the cards
		Card tmpCard = annealerNowGrid[position1[0]][position1[1]];
		annealerNowGrid[position1[0]][position1[1]] = annealerNowGrid[position2[0]][position2[1]];
		annealerNowGrid[position2[0]][position2[1]] = tmpCard;
		//		PokerSquares.printGrid(annealerNowGrid);
	}

	public void annealerUndo() {
		for (int row = 0; row < SIZE; row++)  //Start copy the previous grid in case of backtrack
			for (int col = 0; col < SIZE; col++)
				annealerNowGrid[row][col] = annealerPreviousGrid[row][col];

	}

	public double annealerEnergy() {
		int max = Integer.MIN_VALUE;
		max = PokerSquares.getScore(annealerNowGrid);
		return -max;
	}

	public Card[][] annealerClone() {
		Card[][] tmpGrid = new Card[SIZE][SIZE];
		for (int row = 0; row < SIZE; row++)  //Start copy the previous grid in case of backtrack
			for (int col = 0; col < SIZE; col++)
				tmpGrid[row][col] = annealerNowGrid[row][col];
		//		PokerShuffle tmp = new PokerShuffle(tmpGrid);
		return tmpGrid;
	}
	//end annealer methods
	//	public AAFullHousePokerSquarePlayer(int[] control, boolean[] switchingLocation, boolean[] usingMethod, int[] switchingMethod, boolean printControl, boolean turnOn) {
	//		if (turnOn) {
	//			//		control[0] ---- useFlushPriority 1=true 0=false;  //hold the current priority being run
	//			//		control[1] ---- monteCarloFlushPriority 1=true 0=false;  //hold the current priority being used by monteCarlo
	//			//		control[2] ---- allowSwitchPriority 1=true 0=false;  //Control if switching priority is allowed is allowed
	//			//		control[3] ---- allowMonteCarlo 1=true 0=false;  //control if each game can use monteCarlo or not
	//			//      control[4] ---- trialSperEmptySpace 0=useDefault >0=useInput;  control the amount of trials in monteCarlo
	//			useFlushPriority = (control[0] == 1);
	//			monteCarloFlushPriority = (control[1] == 1);
	//			allowSwitchPriority = (control[2] == 1);
	//			allowMonteCarlo = (control[3] == 1);
	//			if (control[4] > 0) trialsPerEmptySpace = control[4];
	//			switchLocation = switchingLocation;
	//			useMethod = usingMethod;
	//			switchMethod = switchingMethod;
	//
	//			//		if (control[0] == 1) useFlushPriority = true; else useFlushPriority = false;	
	//			//		if (control[1] == 1) monteCarloFlushPriority = true; else monteCarloFlushPriority = false;
	//			//		if (control[2] == 1) allowSwitchPriority = true; else allowSwitchPriority = false;
	//			//		if (control[3] == 1) allowMonteCarlo = true; else allowMonteCarlo = false;
	//			//		if (control[4]  > 0) trialSperEmptySpace = control[4];  control the amount of trials in monteCarlo
	//			if (printControl) {
	//				for (int i : control)
	//					System.out.print(" " + i);
	//				System.out.println();
	//			}
	//		}
	//	}

	public static int[] findFreeCol(ArrayList<int[]> arrayList, Card card, int operation, long millisRemaining) {  //Find and return column with most free space using an arrayList of possible locations
		int[] spaceAmount = new int[arrayList.size()];
		int[] tmpLocation = arrayList.get(0);
		if (arrayList.size() == 1)
			return tmpLocation;
		else{
			if (operation == 1)
				tmpLocation = MonteCarlo(cardGrid, card, 0, card.getSuit(), card.getRank(), arrayList, millisRemaining, trialsPerEmptySpace);
			if (operation == 2) {
				for (int e = 0; e < arrayList.size(); e++){ //Total up free space for logical locations
					int count = 0;
					tmpLocation = arrayList.get(e);
					for (int w = 0; w < SIZE; w++){
						if (cardGrid[w][tmpLocation[1]] == null)
							count = count + 1;
					}
					spaceAmount[e] = count;
				}
				for (int i = 0; i < arrayList.size()-1; i++){  //Compare free space to find the right placement location
					if (spaceAmount[i] > spaceAmount[i+1])
						tmpLocation = arrayList.get(i);
					else if(spaceAmount[i] == spaceAmount[i+1]){
						for (int j = 0; j < SIZE; j++){
							if (cardGrid[j][arrayList.get(i)[1]] != null && cardGrid[j][arrayList.get(i+1)[1]] != null) {
								if (cardGrid[j][arrayList.get(i)[1]].getRank() == card.getRank()+1 || cardGrid[j][arrayList.get(i)[1]].getRank() == card.getRank()-1)
									tmpLocation = arrayList.get(i);
								else if (cardGrid[j][arrayList.get(i+1)[1]].getRank() == card.getRank()+1 || cardGrid[j][arrayList.get(i+1)[1]].getRank() == card.getRank()-1)
									tmpLocation = arrayList.get(i+1);
							}
						}
					}
					else 
						tmpLocation = arrayList.get(i+1);

				}
			}
			return tmpLocation;
		}
	}

	public static Card[] movingCard(Card[] array, Card cardThis, int tracker) {  //moving cards in card arrays. return the new array
		for (int i = tracker; i < array.length; i++) {
			if ( array[i].getRank() == cardThis.getRank() && array[i].getSuit() == cardThis.getSuit()) {
				array[i] = array[tracker];
				array[tracker] = cardThis;
				return array;
			}
		}
		return array;
	}

	public static Card[] shuffleCard(Card[] arrayThis, int tracker) {
		while (tracker < arrayThis.length - 1) {
			Card card = arrayThis[tracker];
			int location = random.nextInt(arrayThis.length - tracker) + tracker;
			arrayThis[tracker] = arrayThis[location];
			arrayThis[location] = card;		
			tracker++;
		}
		return arrayThis;
	}

	public static int[] rankColCheck(int row, Card card, Card[][] cardGrid, int[] location, int operation, long millisRemaining){ //Operation"1" uses algo WITH MonteCarlo, Operation"2" use simple algo WITHOUT MonteCarlo
		ArrayList<int[]> rankCalCol = new ArrayList<int[]>();
		for (int col = 0; col < SIZE; col++) { //Find all possible placement location in the row of the card's suit
			if (cardGrid[row][col] == null) { //TODO changed row to card.getSuit
				int[] nullLocation = {card.getSuit(), col};
				rankCalCol.add(nullLocation); //Record coordinates of possible locations in rankCal
			}
		}

		for (int[] item : rankCalCol) {
			if (sameRank(cardGrid, card) != null && item[1] == sameRank(cardGrid, card)[1]) {
				return item;
			}
		}

		for (int i = 0; i < rankCalCol.size(); i++){
			int[] currentLocation = rankCalCol.get(i);
			for (int r = 0; r < SIZE; r++){
				if (cardGrid[r][currentLocation[1]] != null) {
					if (cardGrid[r][currentLocation[1]].getRank() == card.getRank()){ //Find Same rank cards to pair with
						location = currentLocation;
						return location;
					}
				}
				else if (findEmptyCol(cardGrid, rankCalCol) != null) {  //Find empty column to start a new rank
					return location = findEmptyCol(cardGrid, rankCalCol);  //TODO changed
				}
				else {
					if (operation == 1)
						return location = MonteCarlo(cardGrid, card, 1, row, card.getRank(), rankCalCol, millisRemaining, trialsPerEmptySpace);
					if (operation == 3)
						return location = findFreeCol(rankCalCol, card, operation, millisRemaining);  //Find location for junk cards TODO
				}
			}
		}
		return location = MonteCarlo(cardGrid, card, 0, row, card.getRank(), empty, millisRemaining, trialsPerEmptySpace);
	}

	public ArrayList<int[]> suitEmptySpaceLocation(Card[][] cardGridThis, int row){  //find empty space in playGrid
		int[] locationSuit= {0, 0};
		ArrayList<int[]> emptySuitLocation = new ArrayList<int[]>();
		for (int col = 0; col < SIZE; col++){
			if (cardGridThis[row][col] == null){
				locationSuit[0] = row; locationSuit[1] = col;
				emptySuitLocation.add(locationSuit);
			}
		}
		return emptySuitLocation;
	}

	public static int[] findEmptyCol(Card[][] cardGridThis, ArrayList<int[]> arrayList){  //Find and return an empty column by using an arrayList of possible locations
		for (int e = 0; e < arrayList.size(); e++){
			int count = 0;
			int[] tmpLocation = arrayList.get(e);
			for (int w = 0; w < SIZE; w++) {
				if (cardGridThis[w][tmpLocation[1]] == null)
					count = count + 1;
			}
			if (count == SIZE)
				return tmpLocation;
		}
		return null;
	}

	public boolean CardDeckRemove(Card[][] currentGrid, Card card) {  //check if card is already been used
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (currentGrid[row][col] == card) return false;
			}
		}
		return true;
	}

	public static boolean checkFullGrid(Card[][] grid, int amount) {  //Check  any 5x5 grid if it empty
		int count = 0;
		for (int row = 0; row < SIZE; row++) {  //Check for empty space
			for (int col = 0; col < SIZE; col++) {
				if (grid[row][col] != null)
					count++;
			}
		}
		if (count >= amount)
			return true;
		else
			return false;
	}

	public static int countGridEmptySpace(Card[][] grid) {  //Count amount of empty space in a grid
		int count = 0;
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				if (grid[row][col] == null)
					count++;
		return count;
	}

	//	public int[] annealer(Card[][] currentGrid, Card card, ArrayList<int[]> specialArray, long millisRemaining) {  //TODO Use annealer to predict best card
	//		int trials = countGridEmptySpace(currentGrid);
	//		System.out.println("trials" + trials);
	//		//		System.out.println("trials " + trials);
	//		ArrayList<int[]> solidPosition = new ArrayList<int[]>();  //Stores possible locations for the card
	//		for (int row = 0; row < SIZE; row++) {
	//			for (int col = 0; col < SIZE; col++) {
	//				if (currentGrid[row][col] != null) {
	//					int[] tmp = {0, 0};
	//					tmp[0] = row;
	//					tmp[1] = col;
	//					solidPosition.add(tmp);
	//				}
	//			}
	//		}
	//
	//		ArrayList<int[]> emptyPosition = new ArrayList<int[]>();
	//		if (specialArray.size() == 0) {
	//			for (int row = 0; row < SIZE; row++) {
	//				for (int col = 0; col < SIZE; col++) {
	//					if (currentGrid[row][col] == null) {
	//						int[] tmp = {0, 0};
	//						tmp[0] = row;
	//						tmp[1] = col;
	//						emptyPosition.add(tmp);
	//					}
	//				}
	//			}
	//		}
	//		else {
	//			for (int[] loco : specialArray) {
	//				emptyPosition.add(loco);
	//			}
	//		}
	//
	//		if (emptyPosition.size() == 1)
	//			return emptyPosition.get(0);
	//
	//		//		System.out.println("New MONTECARLO");
	//		int[] position = {0,0};  //ever changing position by comparing highest score and currentHigh score
	//		int count = 0;
	//		int currentHigh = 0;  //Hold current score for 1 particular position in the array list of possible position
	//		int highest = 0;  //Hold the highest score so far
	//		Card[][] cardGridTest = new Card[SIZE][SIZE];
	//		annealerNowGrid = new Card[SIZE][SIZE];
	//		annealerPreviousGrid = new Card[SIZE][SIZE];
	//		Card[] deckTest = new Card[allCard.length];  //create a full test deck
	//		int testTracker = officialTracker;
	//		//		System.out.println("Size of possible positions: " + arrayList.size());
	//		while (!emptyPosition.isEmpty()){
	//			//			System.out.println(emptyPosition.size());
	//			//			System.out.println("Using monte arrayList");
	//			currentHigh = 0;
	//			int[] testPos = emptyPosition.get(0);
	//			ArrayList<int[]> testSolidPosition = new ArrayList<int[]>();
	//			for (int[] i : solidPosition) {
	//				int[] tmp = {0, 0};
	//				tmp[0] = i[0];
	//				tmp[1] = i[1];
	//				testSolidPosition.add(tmp);
	//			}
	//			testSolidPosition.add(testPos);
	//			emptyPosition.remove(0);
	//			count = 0;  //reset trials count
	//			deckTest = allCard;  //Reset deckTest for each position test
	//			testTracker = officialTracker;  //reset tracker for each position test	
	//
	//			for (int row = 0; row < SIZE; row++) {  //Clone current play grid for testing
	//				for (int col = 0; col < SIZE; col++) {
	//					cardGridTest[row][col] = currentGrid[row][col];
	//					annealerNowGrid[row][col] = currentGrid[row][col];
	//					annealerPreviousGrid[row][col] = currentGrid[row][col];
	//				}
	//			}
	//
	//			cardGridTest[testPos[0]][testPos[1]] = card;  //Play requested testingCard into grid before starting trials
	//			annealerNowGrid[testPos[0]][testPos[1]] = card;  //Play requested testingCard into grid before starting trials
	//			annealerPreviousGrid[testPos[0]][testPos[1]] = card;  //Play requested testingCard into grid before starting trials
	//			deckTest = movingCard(deckTest, card, testTracker);  //update testDeck because we just played the requested card
	//			testTracker++;  //update testTracker
	//			deckTest = shuffleCard(deckTest, testTracker);  //Shuffle test deck after card play
	//			//			System.out.println("Inside possible arrayList");
	//			while (count < 1) {
	//				//				System.out.println("Inside trial");
	//				//				System.out.println("Inside trial loop");
	//				for (int row = 0; row < SIZE; row++) {
	//					for (int col = 0; col < SIZE; col++) {
	//						if (cardGridTest[row][col] == null) {
	//							Card testCard = deckTest[testTracker];
	//							cardGridTest[row][col] = testCard;
	//							annealerNowGrid[row][col] = testCard;
	//							annealerPreviousGrid[row][col] = testCard;
	//							deckTest = movingCard(deckTest, testCard, testTracker);
	//							testTracker++;
	//						}
	//					}
	//				}
	//
	//				for (int row = 0; row < SIZE; row++) {
	//					for (int col = 0; col < SIZE; col++) {
	//						if (cardGridTest[row][col] == null) {
	//							annealerNowGrid[row][col] = cardGridTest[row][col];
	//							annealerPreviousGrid[row][col] = cardGridTest[row][col];
	//						}
	//					}
	//				}
	//
	//				energy = annealerEnergy();
	//				minState = annealerClone();
	//				minEnergy = annealerEnergy();
	//				double temperature = initialTemperature;
	//				for (int i = 0; i < 1000000; i++) {
	//					//					if (i % 100000 == 0) 
	//					//						System.out.println(minEnergy + "\t" + energy);
	//					annealerStep(testSolidPosition);  //take a step
	//					double nextEnergy = annealerEnergy();  //find the energy of that step
	//					if (nextEnergy <= energy  || random.nextDouble() < Math.exp((energy - nextEnergy)/temperature)) {  //if it good energy, accept it
	//						energy = nextEnergy;
	//						if (nextEnergy < minEnergy) {  //if it the best we seen, store it
	//							minState = annealerClone();
	//							minEnergy = nextEnergy;
	//						}
	//					}
	//					else  //if the step sucked
	//						annealerUndo();  //undo our step
	//					temperature *= temperatureDecay;
	//				}
	//				System.out.println(PokerSquares.getScore(minState));
	//				currentHigh = currentHigh + PokerSquares.getScore(minState);
	//
	//				deckTest = allCard;  //Reset deckTest for each position test
	//				testTracker = officialTracker;  //reset tracker for each position test	
	//
	//				for (int row = 0; row < SIZE; row++) {  //Clone current play grid for testing
	//					for (int col = 0; col < SIZE; col++)
	//						cardGridTest[row][col] = currentGrid[row][col];
	//				}
	//
	//
	//				cardGridTest[testPos[0]][testPos[1]] = card;  //Play requested testingCard into grid before starting trials
	//				deckTest = movingCard(deckTest, card, testTracker);  //update testDeck because we just played the requested card
	//				testTracker++;  //update testTracker
	//				deckTest = shuffleCard(deckTest, testTracker);  //Shuffle test deck after card play
	//				count++;
	//
	//				if (isOutOfTime(millisRemaining) == true) {  //get out of monte carlo if run out of time
	//					count = trials;
	//				}
	//			}
	//
	//			//			System.out.println("CurrentHigh:" + currentHigh + " " + highest);
	//			if (currentHigh > highest) {
	//				//				System.out.println("got high trial");
	//				position = testPos;
	//				highest = currentHigh;
	//			}
	//		}
	//		return position;
	//	}

	public static int[] MonteCarlo (Card[][] currentGrid, Card card, int nullOption, int nullRow, int nullCol, ArrayList<int[]> specialArray, long millisRemaining, int trialsPerEmptySpace) {  //TODO monte carlo thing
		boolean useNullOption = true;
		if (specialArray.size() == 1)  //make things more efficient by saying that there is no need for monteCarlo if there is only one placement option
			return specialArray.get(0);

		int trials = trialsPerEmptySpace * countGridEmptySpace(currentGrid);
		ArrayList<int[]> arrayList = new ArrayList<int[]>();  //Stores possible locations for the card
		for (int[] loco : specialArray) {
			arrayList.add(loco);
			useNullOption = false;
		}
		if (useNullOption == true) {
			if (nullOption == 1) {
				for (int col = 0; col < SIZE; col++) { //Scan through the current playGrid for possible location in a given row to place the requested card of a particular suit
					if (currentGrid[nullRow][col] == null) {
						int[]tmpPosition = {0, 0};
						tmpPosition[0] = nullRow;
						tmpPosition[1] = col;
						arrayList.add(tmpPosition);
					}
				}
			}
			if (nullOption == 2) {
				for (int row = 0; row < SIZE; row++){  //Scan through the current playGrid for possible location in a given column to place the requested card of a particular suit
					if (currentGrid[row][nullCol] == null) {
						int[]tmpPosition = {0, 0};
						tmpPosition[0] = row;
						tmpPosition[1] = nullCol;
						arrayList.add(tmpPosition);
					}
				}
			}
			else {  //nullOption 0 as default
				for (int row = 0; row < SIZE; row++){
					for (int col = 0; col < SIZE; col++) { //Scan through the current playGrid for possible location to place the requested card of a particular suit
						if (currentGrid[row][col] == null) {
							int[]tmpPosition = {0, 0};
							tmpPosition[0] = row;
							tmpPosition[1] = col;
							arrayList.add(tmpPosition);
						}

					}
				}
			}
		}
		int[] position = {0,0};  //ever changing position by comparing highest score and currentHigh score
		int count = 0;
		int currentHigh = 0;  //Hold current score for 1 particular position in the array list of possible position
		int highest = 0;  //Hold the highest score so far
		Card[][] cardGridTest = new Card[SIZE][SIZE];
		Card[] deckTest = new Card[allCard.length];  //create a full test deck
		int testTracker = officialTracker;
		monteCarloFlushPriority = useFlushPriority;
		while (!arrayList.isEmpty()){
			monteCarloFlushPriority = useFlushPriority;
			currentHigh = 0;
			int[] testPos = arrayList.get(0);
			arrayList.remove(0);
			count = 0;  //reset trials count
			deckTest = allCard;  //Reset deckTest for each position test
			testTracker = officialTracker;  //reset tracker for each position test	

			for (int row = 0; row < SIZE; row++) {  //Clone current play grid for testing
				for (int col = 0; col < SIZE; col++)
					cardGridTest[row][col] = currentGrid[row][col];
			}

			cardGridTest[testPos[0]][testPos[1]] = card;  //Play requested testingCard into grid before starting trials
			deckTest = movingCard(deckTest, card, testTracker);  //update testDeck because we just played the requested card
			testTracker++;  //update testTracker
			deckTest = shuffleCard(deckTest, testTracker);  //Shuffle test deck after card play
			while (count < trials) {
				monteCarloFlushPriority = useFlushPriority;
				while(checkFullGrid(cardGridTest, 25) == false) {  //check for end game
					Card testCard = deckTest[testTracker];
					int[] tmpPos = testPos;

					if (monteCarloFlushPriority) {
						tmpPos = flushPriority(testCard, cardGridTest, 3, millisRemaining);
					}
					else if(!monteCarloFlushPriority) {
						tmpPos = fullHousePriority(testCard, cardGridTest, 3, millisRemaining);  //recursively call algo method.... infinite loop problem
					}
					else {
						System.out.println("Error: MonteCarlo can't decide which algorithm to use.");
					}

					cardGridTest[tmpPos[0]][tmpPos[1]] = testCard;
					deckTest = movingCard(deckTest, testCard, testTracker);
					testTracker++;
					//					System.out.println("current tmpPos: " + tmpPos[0] + " " + tmpPos[1]);
				}
				currentHigh = currentHigh + PokerSquares.getScore(cardGridTest);

				deckTest = allCard;  //Reset deckTest for each position test
				testTracker = officialTracker;  //reset tracker for each position test	

				for (int row = 0; row < SIZE; row++) {  //Clone current play grid for testing
					for (int col = 0; col < SIZE; col++)
						cardGridTest[row][col] = currentGrid[row][col];
				}

				cardGridTest[testPos[0]][testPos[1]] = card;  //Play requested testingCard into grid before starting trials
				deckTest = movingCard(deckTest, card, testTracker);  //update testDeck because we just played the requested card
				testTracker++;  //update testTracker
				deckTest = shuffleCard(deckTest, testTracker);  //Shuffle test deck after card play
				count++;

				if (isOutOfTime(millisRemaining) == true) {  //get out of monte carlo if run out of time
					count = trials;
				}
			}
			if (currentHigh > highest) {
				position = testPos;
				highest = currentHigh;
			}
		}
		return position;
	}

	public static int[] sameRank (Card[][] grid, Card card) {  //Find cards with the same rank in the grid then return the position of that card
		int[] position = {0, 0};
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (grid[row][col] != null && card.getRank() == grid[row][col].getRank()) {
					position[0] = row;
					position[1] = col;
					return position;
				}
			}
		}
		return null;
	}

	public static ArrayList<Integer> colEmptySpace (Card[][] grid, int col) {  //Input a column and find all empty space in that column
		ArrayList<Integer> arrayList = new ArrayList<Integer>();
		int position;
		for (int row = 0; row < SIZE; row++) {
			if (grid[row][col] == null) {
				position = row;
				arrayList.add(position);
			}
		}
		return arrayList;
	}

	public ArrayList<Integer> rowEmptySpace (Card[][] grid, int row) {  //Input a row and find all empty space in that row
		ArrayList<Integer> arrayList = new ArrayList<Integer>();
		int position;
		for (int col = 0; col < SIZE; col++) {
			if (grid[row][col] == null) {
				position = col;
				arrayList.add(position);
			}
		}
		return arrayList;
	}

	public static int sameSuit (Card[][] grid, ArrayList<Integer> emptySpace, Card card) {  //return the row number
		for (int i = 0; i < emptySpace.size(); i++) {  //for every item in the emptySpace list...
			int itemTmp = emptySpace.get(i);
			for (int col = 0; col < SIZE; col++) {  //Scan through all place in the same row as the item
				if (grid[itemTmp][col] != null && grid[itemTmp][col].getSuit() == card.getSuit()) {  //If there is a card
					return itemTmp;  //Return the item
				}
			}
		}
		return -1;
	}

	public ArrayList<Integer> pureSuit (Card[][] grid, Card card) {  //return list of row that have only the same suit (NO MIX) as the card and have at least 1 free space
		ArrayList<Integer> rows = new ArrayList<Integer>();
		int cardSuit = card.getSuit();
		for (int row = 0; row < SIZE; row++) {
			boolean isGood = true;
			int count = 0;
			for (int col = 0; col < SIZE; col++) {  //scan through all item in a row
				if (grid[row][col] != null && grid[row][col].getSuit() != cardSuit)  //if an item is not the same suit, mark it as bad suit
					isGood = false;
				if (grid[row][col] != null)  //count the amount of card in this row
					count++;
			}
			if (isGood && count < SIZE)  //if the row is good and have at least 1 empty space, add it to the list
				rows.add(row);
		}
		return rows;  //return list of good rows
	}

	public static ArrayList<Integer> possibleFlushRow(Card[][] grid, int freeSpace) {  //return a list of row that can still be flush and have at least the specified freeSpace
		ArrayList<Integer> possibleFlushRow = new ArrayList<Integer>();
		for (int row = 0; row < SIZE; row++) {
			ArrayList<Integer> suit = new ArrayList<Integer>();
			int space = 0;
			for (int col = 0; col < SIZE; col++) {
				if (grid[row][col] != null && !suit.contains(grid[row][col].getSuit()))
					suit.add(grid[row][col].getSuit());
				if (grid[row][col] == null)
					space++;	
			}
			if (space == freeSpace && suit.size() <= 1)
				possibleFlushRow.add(row);
		}
		return possibleFlushRow;
	}

	public ArrayList<Integer> possibleFour(Card[][] grid, int freeSpace) {  //return a list of column that can still be possible four-of-a-kind and have at least the specified freeSpace
		ArrayList<Integer> possibleFour = new ArrayList<Integer>();
		boolean noFreeSpace = false;
		if (freeSpace == -1)
			noFreeSpace = true;
		for (int col = 0; col < SIZE; col++) {
			int space = 0;
			int[] rank = new int[13];
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][col]  != null)
					rank[grid[row][col].getRank()]++;
				else
					space++;
			}
			for (int i : rank)
				if (i == 3 && (space == freeSpace || noFreeSpace)) {
					possibleFour.add(col);
				}
		}
		return possibleFour;
	}

	public ArrayList<Integer> possibleFullHouse(Card[][] grid) {  //return a list of column that can still be possible fullHouse
		ArrayList<Integer> possibleFullHouse = new ArrayList<Integer>();
		for (int col = 0; col < SIZE; col++) {
			int[] rank = new int[13];
			int count = 0;
			int set = 0;
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][col] != null)
					rank[grid[row][col].getRank()]++;
			}
			for (int i : rank) {
				if (i == 2)
					set++;
				if (i > 0)
					count++;
			}
			if (count == 2 && set == 2)
				possibleFullHouse.add(col);
		}
		return possibleFullHouse;
	}

	public static int emptyCol (Card[][] grid) {  //find completely empty column
		int count = 0;
		for (int col = 0; col < SIZE; col++) {
			count = 0;
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][col] == null)
					count++;
			}
			if (count == SIZE)  //if there are 5 empty space in a column, return that column
				return col;
		}
		return -1;  //return -1 if no empty col
	}

	public static int emptyRow (Card[][] grid) {  //find completely empty row
		int count = 0;
		for (int row = 0; row < SIZE; row++) {
			count = 0;
			for (int col = 0; col < SIZE; col++) {
				if (grid[row][col] == null)
					count++;
			}
			if (count == SIZE)  //if there are 5 empty space in a column, return that column
				return row;
		}
		return -1;  //return -1 if no empty row
	}

	public static ArrayList<Integer> bestRankCol (Card[][] grid) {  //find column with only one rank assuming that each column have at least 1 card
		ArrayList<Integer> ranks = new ArrayList<Integer>();
		ArrayList<Integer> column = new ArrayList<Integer>();
		for (int col = 0; col < SIZE; col++) {
			ranks.clear();
			for (int row = 0; row < SIZE; row++) {  //go through all places in a column
				if (grid[row][col] != null) {  //when ever you find a card in a particular column
					if (ranks.contains(grid[row][col].getRank()) == false) {  //if a rank have not been recorded yet
						ranks.add(grid[row][col].getRank());  //record the rank
					}
				}
			}
			if (ranks.size() < 2)  //return the column that occupied by only one rank
				column.add(col);
		}
		return column;
	}

	public static int worstColAmount (Card[][] grid) {  //find column with the most amount of different ranks, return that amount to compare later
		//		int currentCol = -1;  //keep track of the worst column
		int amount = 1;  //keep track of the worst amount
		ArrayList<Integer> ranks = new ArrayList<Integer>();
		for (int col = 0; col < SIZE; col++) {
			ranks.clear();
			while (isFull(grid, col, -1) == true) {
				if (col >= 5)
					return amount;
				col++;
			}
			for (int row = 0; row < SIZE; row++) {  //go through all places in a column
				if (grid[row][col] != null) {  //when ever you find a card in a particular column
					if (ranks.contains(grid[row][col].getRank()) == false)  //if a rank have not been recorded yet
						ranks.add(grid[row][col].getRank());  //record the rank
				}
			}
			if (ranks.size() > amount) { //return the column that occupied by only one rank
				//				currentCol = col;
				amount = ranks.size();
			}
		}
		return amount;
	}

	public static boolean suitEmptySpace(Card[][] cardGridThis, int row){  //find empty space in playGrid
		for (int col = 0; col < SIZE; col++){
			if (cardGridThis[row][col] == null)
				return true;
		}
		return false;
	}

	public static boolean isFull (Card[][] grid, int loco, int rowORcol) {  //1 means row, -1 means col, loco store either the row or col being tested
		if (loco >= SIZE )
			return true;

		int count = 0;
		if (rowORcol == 1) {
			for (int col = 0; col < SIZE; col++) {
				if (grid[loco][col] != null)
					count++;
			}
		}
		if (rowORcol == -1)
			for (int row = 0; row < SIZE; row++)
				if (grid[row][loco] != null)
					count++;

		if (count == SIZE)
			return true;
		else
			return false;
	}

	public static ArrayList<Integer> worstColList (Card[][] grid) {
		ArrayList<Integer> cols = new ArrayList<Integer>();
		int amount = worstColAmount(grid);
		ArrayList<Integer> ranks = new ArrayList<Integer>();
		for (int col = 0; col < SIZE; col++) {
			ranks.clear();
			for (int row = 0; row < SIZE; row++) {  //go through all places in a column
				if (grid[row][col] != null) {  //when ever you find a card in a particular column
					if (ranks.contains(grid[row][col].getRank()) == false) {  //if a rank have not been recorded yet
						ranks.add(grid[row][col].getRank());  //record the rank
					}
				}
			}
			if (ranks.size() == amount && isFull(grid, col, -1) == false) //return the column that occupied by only one rank
				cols.add(col);
		}		
		return cols;
	}

	public static ArrayList<Integer> mostSpaceCol (Card[][] grid, ArrayList<Integer> columnList) {
		ArrayList<Integer> mostSpaceCol = new ArrayList<Integer>();
		int count = 0;
		int highestCount = 1;
		for (int col : columnList) {
			count = 0;
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][col] == null) {
					count++;
				}
			}
			if (count == highestCount) {
				mostSpaceCol.add(col);
			}
			else if (count > highestCount) {
				highestCount = count;
				mostSpaceCol.clear();
				mostSpaceCol.add(col);
			}
		}
		return mostSpaceCol;
	}

	public static ArrayList<int[]> emptySpace (Card[][] grid) {
		ArrayList<int[]> emptySpace = new ArrayList<int[]>();
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				if (grid[row][col] == null) {
					int[] position = {row, col};
					emptySpace.add(position);
				}
		return emptySpace;
	}

	public ArrayList<Integer> rankCountCol(Card[][] grid, Card card) {  //get columns that have the most similar rank with the current card  //TODO
		ArrayList<Integer> rankCountCol = new ArrayList<Integer>();
		int currentRank = card.getRank();
		int highCount = 1;
		int count = 0;
		for (int col = 0; col < SIZE; col++) {
			count = 0;
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][col] != null && grid[row][col].getRank() == currentRank) {
					count++;
				}
			}
			if (count == highCount)
				rankCountCol.add(col);

			if (count > highCount) {
				highCount = count;
				rankCountCol.clear();
				rankCountCol.add(col);
			}
		}
		return rankCountCol;
	}

	public static int[] fullHousePriority(Card card, Card[][] playGrid, int operation, long millisRemaining){ //input a card, then calculate the location to place the card
		int[] position= {0, 0};
		if (isOutOfTime(millisRemaining) == true)  //check time to decide on monte carlo use
			operation = 3;
		if (sameRank(playGrid, card) != null && colEmptySpace(playGrid, sameRank(playGrid, card)[1]).isEmpty() != true) {  //if there is another card exists with the same rank as current card and there are room to place this current card in the same column
			ArrayList<Integer> sameRankList = new ArrayList<Integer>();  //Store all row of a particular column
			position[1] = sameRank(playGrid, card)[1];
			sameRankList = colEmptySpace(playGrid, sameRank(playGrid, card)[1]);  //Find empty space in the column that contain a card with same rank as current card
			if (sameSuit(playGrid, sameRankList, card) != -1) {  //pick the position that would pair the card for its rank and its suit up
				position[0] = sameSuit(playGrid, sameRankList, card);
				return position;
			}
			else if (sameSuit(playGrid, sameRankList, card) == -1) {  //if there is no free space that would pair this card to its suit row
				if (emptyRow(playGrid) != -1) {
					position[0] = emptyRow(playGrid);
					return position;
				}
				else {
					if (operation == 1) {
						ArrayList<int[]> monteSameRankList = new ArrayList<int[]>();
						int[] tPosition = {0, 0};
						tPosition[1] = position[1];
						for (int row : sameRankList) {
							tPosition[0] = row;
							monteSameRankList.add(tPosition);
						}
						return position = MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteSameRankList, millisRemaining, trialsPerEmptySpace);
					}
					if (operation == 3) {  //randomly pick a space in the free space area
						position[0] = sameRankList.get(random.nextInt(sameRankList.size()));		
						return position;
					}
				}
			}
		}
		else if (sameRank(playGrid, card) == null && emptyCol(playGrid) != -1) {  //if There is no card with the same rank but there is an empty column.
			ArrayList<Integer> emptySpaceCol = new ArrayList<Integer>();
			position[1] = emptyCol(playGrid);
			emptySpaceCol = colEmptySpace(playGrid, emptyCol(playGrid));  //find all empty space in the empty column
			if (sameSuit(playGrid, emptySpaceCol, card) != -1) {  //Find similar suit area
				position[0] = sameSuit(playGrid, emptySpaceCol, card);
				return position;
			}
			else if (sameSuit(playGrid, emptySpaceCol, card) == -1) {  //if there is no row with the same suit
				if (emptyRow(playGrid) != -1) {  //if there is an empty row
					position[0] = emptyRow(playGrid);
					//					position[1] = emptyCol(playGrid);
					return position;  //choose the new suit and rank position for this unique card
				}
				else {  //if there is no empty row
					if (operation == 1) {
						ArrayList<int[]> monteEmptySpaceCol = new ArrayList<int[]>();
						int[] tPosition = {0,0};
						tPosition[1] = position[1];
						for (int row : colEmptySpace(playGrid, position[1])) {
							tPosition[0] = row;
							monteEmptySpaceCol.add(tPosition);
						}
						return position = MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteEmptySpaceCol, millisRemaining, trialsPerEmptySpace);
					}
					if (operation == 3) {
						position[0] = emptySpaceCol.get(random.nextInt(emptySpaceCol.size()));
						return position;
					}
				}
			}
		}
		else if (sameRank(playGrid, card) == null && emptyCol(playGrid) == -1) { //if there is no card with the same rank and no empty column for this new card's rank
			if (bestRankCol(playGrid).isEmpty() == false) {  //if there is a column with only 1 card rank, store column number
				for (int i = 0; i < bestRankCol(playGrid).size(); i++) {  //go through the list of column with only 1 card rank and find the column that have an empty space intersecting similar suit
					position[1] =  bestRankCol(playGrid).get(i);
					ArrayList<Integer> tmpRow = colEmptySpace(playGrid, position[1]);
					if (sameSuit(playGrid, tmpRow, card) != -1) {
						position[0] = sameSuit(playGrid, tmpRow, card);
						return position;
					}
				}
				if (emptyRow(playGrid) != -1) {  //check if there is an empty row
					for (int i = 0; i < bestRankCol(playGrid).size(); i++) {  //go through the list of column with only 1 card rank and find an empty row
						int currentCol = bestRankCol(playGrid).get(i);
						if (playGrid[emptyRow(playGrid)][currentCol] == null) {
							position[0] = emptyRow(playGrid);
							position[1] = currentCol;
							return position;
						}
					}
				}
				//if there is no empty row
				if (operation == 1) {
					ArrayList<int[]> monteEmptySpaceCol = new ArrayList<int[]>();
					int[] tPosition = {0, 0};
					for (int col : bestRankCol(playGrid)) {
						tPosition[1] = col;
						for (int row : colEmptySpace(playGrid, col)) {
							tPosition[0] = row;
							monteEmptySpaceCol.add(tPosition);
						}
					}
					return position = MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteEmptySpaceCol, millisRemaining, trialsPerEmptySpace);
				}
				if (operation == 3) {
					position[1] = bestRankCol(playGrid).get(random.nextInt(bestRankCol(playGrid).size()));  //randomly choose a column with only 1 rank occupying it
					position[0] = colEmptySpace(playGrid, position[1]).get(random.nextInt(colEmptySpace(playGrid, position[1]).size()));  //randomly choose an empty space in the randomly chosen column
					return position;
				}
			}
			else if (bestRankCol(playGrid).isEmpty() == true) {  //if there is no column with only 1 card rank
				ArrayList<Integer> junkCols = new ArrayList<Integer>();  //open storage for a list of junk column
				junkCols = worstColList(playGrid);  //get a list of junk column
				if (junkCols.size() == 1) {  //there is only 1 column of junk card
					position[1] = junkCols.get(0);  //We already know which column this card will be place in, just need to find the right location
					ArrayList<Integer> tmpEmptyRow = colEmptySpace(playGrid, junkCols.get(0));
					for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
						for (int col = 0; col < SIZE; col++) {  //scan through those rows
							if (playGrid[tmpEmptyRow.get(i)][col] != null && playGrid[tmpEmptyRow.get(i)][col].getSuit() == card.getSuit()) {  //find same suit
								position[0] = tmpEmptyRow.get(i);
								return position;
							}		
						}
					}
					for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
						for (int col = 0; col < SIZE; col++) {  //scan through those rows
							if (playGrid[tmpEmptyRow.get(i)][col] != null && playGrid[tmpEmptyRow.get(i)][col].getRank() == card.getRank()) {  //find same rank
								position[0] = tmpEmptyRow.get(i);
								return position;
							}		
						}
					}
					if (operation == 1) {
						ArrayList<int[]> monteTmpEmptyRow = new ArrayList<int[]>();
						int[] tPosition = {0, 0};
						tPosition[1] = position[1];
						for (int row : tmpEmptyRow) {
							tPosition[0] = row;
							monteTmpEmptyRow.add(tPosition);
						}
						return position = MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteTmpEmptyRow, millisRemaining, trialsPerEmptySpace);
					}
					if (operation == 3) {
						position[0] = tmpEmptyRow.get(random.nextInt(tmpEmptyRow.size())); //TODO check this and following if statement for negative returns
						return position;
					}
				}
				else {  //If there are multiple column tie for worst column
					if (mostSpaceCol(playGrid, junkCols).size() == 1) {  //if there is only one column that have the largest free space
						position[1] = mostSpaceCol(playGrid, junkCols).get(0);  //set position to that column
						ArrayList<Integer> tmpEmptyRow = colEmptySpace(playGrid, position[1]);
						for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
							for (int col = 0; col < SIZE; col++) {  //scan through those rows
								if (playGrid[tmpEmptyRow.get(i)][col] != null && playGrid[tmpEmptyRow.get(i)][col].getSuit() == card.getSuit()) {  //find same suit
									position[0] = tmpEmptyRow.get(i);
									return position;
								}		
							}
						}
						for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
							for (int col = 0; col < SIZE; col++) {  //scan through those rows
								if (playGrid[tmpEmptyRow.get(i)][col] != null && playGrid[tmpEmptyRow.get(i)][col].getRank() == card.getRank()) {  //find same rank
									position[0] = tmpEmptyRow.get(i);
									return position;
								}		
							}
						}
						if (operation == 1) {
							ArrayList<int[]> monteTmpEmptyRow = new ArrayList<int[]>();
							int[] tPosition = {0, 0};
							tPosition[1] = position[1];
							for (int row : tmpEmptyRow) {
								tPosition[0] = row;
								monteTmpEmptyRow.add(tPosition);
							}
							return position = MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteTmpEmptyRow, millisRemaining, trialsPerEmptySpace);
						}
						if (operation == 3) {
							position[0] = tmpEmptyRow.get(random.nextInt(tmpEmptyRow.size()));
							return position;
						}
					}
					else if (mostSpaceCol(playGrid, junkCols).size() > 1) {
						ArrayList<Integer> tmpSpaceCols = new ArrayList<Integer>();
						tmpSpaceCols = mostSpaceCol(playGrid, junkCols);
						for (int spaceCol : tmpSpaceCols) {
							position[1] = spaceCol;  //set position to that column
							ArrayList<Integer> tmpEmptyRow = colEmptySpace(playGrid, position[1]);
							for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
								for (int col = 0; col < SIZE; col++) {  //scan through those rows
									if (playGrid[tmpEmptyRow.get(i)][col] != null && playGrid[tmpEmptyRow.get(i)][col].getSuit() == card.getSuit()) {  //find same suit
										position[0] = tmpEmptyRow.get(i);
										return position;
									}		
								}
							}
							for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
								for (int col = 0; col < SIZE; col++) {  //scan through those rows
									if (playGrid[tmpEmptyRow.get(i)][col] != null && playGrid[tmpEmptyRow.get(i)][col].getRank() == card.getRank()) {  //find same rank
										position[0] = tmpEmptyRow.get(i);
										return position;
									}		
								}
							}
						}
						if (operation == 1) {
							ArrayList<int[]> monteTmpSpaceCols = new ArrayList<int[]>();
							int[] tPosition = {0, 0};
							for (int col : tmpSpaceCols) {
								tPosition[1] = col;
								for (int row : colEmptySpace(playGrid, col)) {
									tPosition[0] = row;
									monteTmpSpaceCols.add(tPosition);
								}
							}
							return position = MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteTmpSpaceCols, millisRemaining, trialsPerEmptySpace);
						}
						if (operation == 3) {
							position[1] = tmpSpaceCols.get(random.nextInt(tmpSpaceCols.size()));
							ArrayList<Integer> tmpEmptyRow = colEmptySpace(playGrid, position[1]);
							position[0] = tmpEmptyRow.get(random.nextInt(tmpEmptyRow.size()));
							return position;
						} 
					}
					if (operation == 1)
						return position = MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), empty, millisRemaining, trialsPerEmptySpace);
					if (operation == 3)
						return position = emptySpace(playGrid).get(random.nextInt(emptySpace(playGrid).size()));
				}
				if (operation == 1) {
					ArrayList<int[]> possibleSpaces = new ArrayList<int[]>();
					int[] tPosition = {0, 0};
					for (int col : junkCols) {
						tPosition[1] = col;
						for (int row : colEmptySpace(playGrid, col)) {
							tPosition[0] = row;
							possibleSpaces.add(tPosition);
						}
					}
					return position = MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), possibleSpaces, millisRemaining, trialsPerEmptySpace);
				}
				if (operation == 3) {
					position[1] = junkCols.get(random.nextInt(junkCols.size()));
					position[0] = colEmptySpace(playGrid, position[1]).get(random.nextInt(colEmptySpace(playGrid, position[1]).size()));
					return position;
				}
			}
		}
		else {  //if nothing fits
			ArrayList<Integer> possibleFlushRow = new ArrayList<Integer>();
			possibleFlushRow = possibleFlushRow(playGrid, 1);


			ArrayList<int[]> allEmptyLocation = new ArrayList<int[]>();
			for (int row = 0; row < SIZE; row++) {
				if (possibleFlushRow.contains(row) == false) {
					for (int col = 0; col < SIZE; col++) {
						if (playGrid[row][col] == null) {
							int[] tmp = {row, col};
							allEmptyLocation.add(tmp);
						}
					}
				}
			}

			if (allEmptyLocation.isEmpty()) {
				for (int row = 0; row < SIZE; row++) {
					for (int col = 0; col < SIZE; col++) {
						if (playGrid[row][col] == null) {
							int[] tmp = {row, col};
							allEmptyLocation.add(tmp);
						}
					}
				}
			}
			if (operation == 1)
				return position = MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), allEmptyLocation, millisRemaining, trialsPerEmptySpace);
			if (operation == 3)
				return position = allEmptyLocation.get(random.nextInt(allEmptyLocation.size()));
		}
		//				printArray2dCards(playGrid);
		return position;
	}

	public static boolean isOutOfTime (long millisRemaining) {  //Check current run time
		currentTime = System.currentTimeMillis();
		millisRem = System.currentTimeMillis() - startTime;
		if (millisRem > 60000 - timeCutOff) {
			return true;
		}
		return false;
	}

	public static int[] flushPriority(Card card, Card[][] playGrid, int operation, long millisRemaining) {
		int[] position= {0, 0};
		if (isOutOfTime(millisRemaining) == true)  //check time to decide on monte carlo use
			operation = 3;
		if (suitEmptySpace(playGrid, card.getSuit()) == true){  //Check if suit row have room
			//									position = suitEmptySpaceLocation(playGrid, c.getSuit(), position); //give the first location of suit row empty room
			return position = rankColCheck(card.getSuit(), card, playGrid, position, operation, millisRemaining);  //find column location for best placement
		}
		else{  //there is no room in suit rows (if there is only free space in junk row)
			if (operation == 1)
				return position = MonteCarlo(playGrid, card, operation, card.getSuit(), card.getRank(), empty, millisRemaining, trialsPerEmptySpace);
			//			if (operation == 2)
			//				return position = annealer(cardGrid, card, 1, card.getSuit(), card.getRank(), empty, millisRemaining);
			if (operation == 3) {
				ArrayList<int[]> tmpArray = new ArrayList<int[]>();  //array to store the free space in junk row
				for (int row = 0;row < SIZE; row++){  //Go through to look for left over empty space
					for (int col = 0; col < SIZE; col++){
						if (playGrid[row][col] == null){
							int[] tmpNull = {row, col};
							tmpArray.add(tmpNull);  //store all free space
						}
					}
				}
				return position = tmpArray.get(random.nextInt(tmpArray.size()));  //randomly pick a free space
			}
		}
		return position;
	}

	public int[] mobileSwitching(Card[][] grid, Card card, int operation, long millisRemaining) {
		int[] position = {-1, -1};
		if (operation == 1 && switchToFlush(grid) == true) {
			monteCarloFlushPriority = true;
			return position = flushPriority(card, grid, operation, millisRemaining);
		}
		if (operation == 3 && switchToFlush(grid) == true) {  //TODO switchFlush
			useFlushPriority = true;
			return position = algo(card, grid, operation, millisRemaining);
		}

		return position;
	}

	public boolean switchToFlush (Card[][] grid) {
		//get int array with the individual hand scores of rows 0 through 4 followed by columns 0 through 4
		int[] handScore = PokerSquares.getHandScores(grid);
		int count = 0;
		int col = 0;

		//TODO, what if we switch when amount of possible flush is greater than amount of possible (fullHouse+four of a kind)?
		//- or switch if a percentage of the grid have been taken up and the score is still low

		//		for (int i = 5; i < 2*SIZE; i++) {
		//			//			if (handScore[i] >= 10 && !isFull(grid, col, -1))  //TODO the ';' made 6 points different in average
		//			count++;
		//
		//			col++;
		//		}
		//TODO flush and monteCarlo make illegal move somehow
		if ((possibleFullHouse(grid).size() + possibleFour(grid, 1).size() + possibleFour(grid, 2).size()) < possibleFlushRow(grid, 1).size()) {
			return allowSwitchPriority;
		}
		if (PokerSquares.getScore(grid) < 25) {
			return allowSwitchPriority;
		}
		if (possibleFlushRow(grid, 2).size() <= 2 ) {
			return allowSwitchPriority;
		}
		if (PokerSquares.getScore(grid) <= 60)  { //Guard against a good grid from switching  // +1 avg. if we don't use this...
			return allowSwitchPriority;
		}
		return false;
	}

	public int[] algo(Card card, Card[][] playGrid, int operation, long millisRemaining) {
		int[] position = {0, 0};

		if (possibleFour(playGrid, -1).size() != 0) {
			position[1] = possibleFour(playGrid, -1).get(0);
			ArrayList<int[]> montePossibleFour = new ArrayList<int[]>();
			ArrayList<Integer> colEmptySpace = colEmptySpace(playGrid, position[1]);
			if (colEmptySpace.size() == 1) {
				position[0] = colEmptySpace.get(0);
				return position;
			}
			for (int item : colEmptySpace) {
				int[] tmp = {item, position[1]};
				montePossibleFour.add(tmp);
			}
			return position = MonteCarlo(playGrid, card, 0, -1, -1, montePossibleFour, millisRemaining, trialsPerEmptySpace);
		}

		if (!useFlushPriority)
			return position = fullHousePriority(card, playGrid, operation, millisRemaining);
		else if (useFlushPriority)
			return position = flushPriority(card, playGrid, operation, millisRemaining);
		else
			System.out.println("Error: algo.UseFlushBooleanProblem");
		return position;
	}

	@Override
	public int[] getPlay(Card card, long millisRemaining) {
		//				System.out.println(cardNames(card.getRank(), card.getSuit()));
		//				System.out.println("Current Official Play Card: " + cardNames(card.getRank(), card.getSuit()));
		//				System.out.println("Card Suit" + card.getSuit());
		//		int[] playPos = flushPriority(card, cardGrid, 3, millisRemaining); //position given in terms of a array.
		//		int[] playPos = flushPriority(card, cardGrid, 1, millisRemaining);
		int[] playPos = fullHousePriority(card, cardGrid, 3, millisRemaining);
		//		System.out.println("Remaining: " + millisRemaining);
		cardGrid[playPos[0]][playPos[1]] = card;
		allCard = movingCard(allCard, card, officialTracker);
		officialTracker++;
		return playPos;
	}

	public static void main(String[] args) {
		//		new PokerSquares(new AAFullHousePokerSquarePlayer(), 60000L).play(); // play a single game
		//		control[0] ---- useFlushPriority 1=true 0=false;  //hold the current priority being run
		//		control[1] ---- monteCarloFlushPriority 1=true 0=false;  //hold the current priority being used by monteCarlo
		//		control[2] ---- allowSwitchPriority 1=true 0=false;  //Control if switching priority is allowed is allowed
		//		control[3] ---- allowMonteCarlo 1=true 0=false;  //control if each game can use monteCarlo or not
		//      control[4] ---- trialSperEmptySpace 0=useDefault >0=useInput;  control the amount of trials in monteCarlo
		new PokerSquares(new AAFullHousePokerSquarePlayer(), 60000L).play(); // play a single game
		//		System.out.println("Switched to flush: " + useFlushPriority);
		//		System.out.println("Allowed monteCarlo: " + allowMonteCarlo);
	}
}
