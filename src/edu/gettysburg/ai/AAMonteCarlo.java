package edu.gettysburg.ai;

import java.util.ArrayList;


public class AAMonteCarlo {
	private final static int SIZE = PokerSquares.SIZE;

	public static int[] MonteCarlo (Card[][] currentGrid, Card card, int nullOption, int nullRow, int nullCol, ArrayList<int[]> specialArray, long millisRemaining, int trialsPerEmptySpace) {  //TODO monte carlo thing
		boolean useNullOption = true;
		if (specialArray.size() == 1)  //make things more efficient by saying that there is no need for monteCarlo if there is only one placement option
			return specialArray.get(0);

//		int trials = trialsPerEmptySpace * (26-AATools.countGridEmptySpace(currentGrid));
		int trials = trialsPerEmptySpace * (25 - AATools.emptySpace(currentGrid).size());
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
						int[]tmpPosition = {row, nullCol};
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
			
			if (arrayList.isEmpty()) {
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
		Card[] deckTest = new Card[AAFinalPokerSquarePlayer.allCard.length];  //create a full test deck
		int testTracker = AAFinalPokerSquarePlayer.officialTracker;
		while (!arrayList.isEmpty()){
			currentHigh = 0;
			int[] testPos = arrayList.get(0);
			arrayList.remove(0);
			count = 0;  //reset trials count
			deckTest = AAFinalPokerSquarePlayer.allCard;  //Reset deckTest for each position test
			testTracker = AAFinalPokerSquarePlayer.officialTracker;  //reset tracker for each position test	
			for (int row = 0; row < SIZE; row++) {  //Clone current play grid for testing
				for (int col = 0; col < SIZE; col++)
					cardGridTest[row][col] = currentGrid[row][col];
			}
			cardGridTest[testPos[0]][testPos[1]] = card;  //Play requested testingCard into grid before starting trials
			deckTest = AATools.movingCard(deckTest, card, testTracker);  //update testDeck because we just played the requested card
			testTracker++;  //update testTracker
			deckTest = AATools.shuffleCard(deckTest, testTracker);  //Shuffle test deck after card play
			while (count < trials) {
				while(AATools.checkFullGrid(cardGridTest, 25) == false) {  //check for end game
					Card testCard = deckTest[testTracker];;
					int[] tmpPos = testPos;
					tmpPos = AAFullHousePokerSquarePlayer.fullHousePriority(testCard, cardGridTest, 3, millisRemaining);
//					if (AAFinalPokerSquarePlayer.monteCarloFlushPriority) {
//						tmpPos = AAFlushOnly.flushOnly(testCard, cardGridTest, 3, millisRemaining);
//						//						System.out.println("monte flush");
//					}
//					else if(!AAFinalPokerSquarePlayer.monteCarloFlushPriority) {
//						tmpPos = AAFullHouse.fullHousePriority(testCard, cardGridTest, 3, millisRemaining);  //recursively call algo method.... infinite loop problem
						//						System.out.println("monte fullhouse");
//					}
//					else {
//						System.out.println("Error: MonteCarlo can't decide which algorithm to use.");
//					}

					cardGridTest[tmpPos[0]][tmpPos[1]] = testCard;
					deckTest = AATools.movingCard(deckTest, testCard, testTracker);
					testTracker++;
				}
				currentHigh = currentHigh + PokerSquares.getScore(cardGridTest);

				deckTest = AAFinalPokerSquarePlayer.allCard;  //Reset deckTest for each position test
				testTracker = AAFinalPokerSquarePlayer.officialTracker;  //reset tracker for each position test	

				for (int row = 0; row < SIZE; row++) {  //Clone current play grid for testing
					for (int col = 0; col < SIZE; col++)
						cardGridTest[row][col] = currentGrid[row][col];
				}

				cardGridTest[testPos[0]][testPos[1]] = card;  //Play requested testingCard into grid before starting trials
				deckTest = AATools.movingCard(deckTest, card, testTracker);  //update testDeck because we just played the requested card
				testTracker++;  //update testTracker
				deckTest = AATools.shuffleCard(deckTest, testTracker);  //Shuffle test deck after card play
				count++;

				if (AATools.isOutOfTime(millisRemaining) == true) {  //get out of monte carlo if run out of time
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
}
