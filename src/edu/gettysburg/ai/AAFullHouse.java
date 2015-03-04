package edu.gettysburg.ai;

import java.util.ArrayList;
import java.util.Random;


public class AAFullHouse {
	private static Random random = new Random();
	private final static int SIZE = PokerSquares.SIZE;
	public static ArrayList<int[]> empty = new ArrayList<int[]>();

	public static int[] fullHousePriority(Card card, Card[][] playGrid, int operation, long millisRemaining){ //input a card, then calculate the location to place the card
		//		System.out.println("Use fullHouse");
		int[] position= {0, 0};
		if (AATools.isOutOfTime(millisRemaining) == true)  //check time to decide on monte carlo use
			operation = 3;
		if (AATools.sameRank(playGrid, card) != null && !AATools.sameRank(playGrid, card).isEmpty() && AATools.colEmptySpace(playGrid, (AATools.sameRank(playGrid, card).get(0))[1]).isEmpty() != true) {  //if there is another card exists with the same rank as current card and there are room to place this current card in the same column
			ArrayList<int[]> sameRankList = new ArrayList<int[]>();  //Store all row of a particular column
			position[1] = AATools.sameRank(playGrid, card).get(0)[1];
			sameRankList = AATools.colEmptySpace(playGrid, AATools.sameRank(playGrid, card).get(0)[1]);  //Find empty space in the column that contain a card with same rank as current card
			if (AATools.sameSuit(playGrid, sameRankList, card) != -1) {  //pick the position that would pair the card for its rank and its suit up
				position[0] = AATools.sameSuit(playGrid, sameRankList, card);
				return position;
			}
			else if (AATools.sameSuit(playGrid, sameRankList, card) == -1) {  //if there is no free space that would pair this card to its suit row
				if (!AATools.emptyRow(playGrid).isEmpty()) {
					position[0] = AATools.emptyRow(playGrid).get(0);
					return position;
				}
				else {
					if (operation == 1) {
						ArrayList<int[]> monteSameRankList = new ArrayList<int[]>();
						int[] tPosition = {0, position[1]};
						for (int[] row : sameRankList) {
							tPosition[0] = row[0];
							monteSameRankList.add(tPosition);
						}
						return position = AAMonteCarlo.MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteSameRankList, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
					}
					if (operation == 3) {  //randomly pick a space in the free space area
						position[0] = sameRankList.get(random.nextInt(sameRankList.size()))[0];		
						return position;
					}
				}
			}
		}
		else if (AATools.sameRank(playGrid, card) == null && !AATools.emptyCol(playGrid).isEmpty()) {  //if There is no card with the same rank but there is an empty column.
			ArrayList<int[]> emptySpaceCol = new ArrayList<int[]>();
			position[1] = AATools.emptyCol(playGrid).get(0);
			emptySpaceCol = AATools.colEmptySpace(playGrid, AATools.emptyCol(playGrid).get(0));  //find all empty space in the empty column
			if (AATools.sameSuit(playGrid, emptySpaceCol, card) != -1) {  //Find similar suit area
				position[0] = AATools.sameSuit(playGrid, emptySpaceCol, card);
				return position;
			}
			else if (AATools.sameSuit(playGrid, emptySpaceCol, card) == -1) {  //if there is no row with the same suit
				if (!AATools.emptyRow(playGrid).isEmpty()) {  //if there is an empty row
					position[0] = AATools.emptyRow(playGrid).get(0);
					return position;  //choose the new suit and rank position for this unique card
				}
				else {  //if there is no empty row
					//TODO Possible useless code
					if (operation == 1) {
						ArrayList<int[]> monteEmptySpaceCol = new ArrayList<int[]>();
						int[] tPosition = {0,position[1]};
						for (int[] row : AATools.colEmptySpace(playGrid, position[1])) {
							tPosition[0] = row[0];
							monteEmptySpaceCol.add(tPosition);
						}
						return position = AAMonteCarlo.MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteEmptySpaceCol, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
					}
					if (operation == 3) {
						position[0] = emptySpaceCol.get(random.nextInt(emptySpaceCol.size()))[0];
						return position;
					}
				}
			}
		}
		else if (AATools.sameRank(playGrid, card) == null && AATools.emptyCol(playGrid).isEmpty()) { //if there is no card with the same rank and no empty column for this new card's rank
			if (AATools.bestRankCol(playGrid).isEmpty() == false) {  //if there is a column with only 1 card rank, store column number
				for (int i = 0; i < AATools.bestRankCol(playGrid).size(); i++) {  //go through the list of column with only 1 card rank and find the column that have an empty space intersecting similar suit
					position[1] =  AATools.bestRankCol(playGrid).get(i);
					ArrayList<int[]> tmpRow = AATools.colEmptySpace(playGrid, position[1]);
					if (AATools.sameSuit(playGrid, tmpRow, card) != -1) {
						position[0] = AATools.sameSuit(playGrid, tmpRow, card);
						return position;
					}
				}
				if (!AATools.emptyRow(playGrid).isEmpty()) {  //check if there is an empty row
					for (int i = 0; i < AATools.bestRankCol(playGrid).size(); i++) {  //go through the list of column with only 1 card rank and find an empty row
						int currentCol = AATools.bestRankCol(playGrid).get(i);
						if (playGrid[AATools.emptyRow(playGrid).get(0)][currentCol] == null) {
							position[0] = AATools.emptyRow(playGrid).get(0);
							position[1] = currentCol;
							return position;
						}
					}
				}
				//if there is no empty row
				if (operation == 1) {
					ArrayList<int[]> monteEmptySpaceCol = new ArrayList<int[]>();
					int[] tPosition = {0, 0};
					for (int col : AATools.bestRankCol(playGrid)) {
						tPosition[1] = col;
						for (int[] row : AATools.colEmptySpace(playGrid, col)) {
							tPosition[0] = row[0];
							monteEmptySpaceCol.add(tPosition);
						}
					}
					return position = AAMonteCarlo.MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteEmptySpaceCol, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
				}
				if (operation == 3) {
					position[1] = AATools.bestRankCol(playGrid).get(random.nextInt(AATools.bestRankCol(playGrid).size()));  //randomly choose a column with only 1 rank occupying it
					position[0] = AATools.colEmptySpace(playGrid, position[1]).get(random.nextInt(AATools.colEmptySpace(playGrid, position[1]).size()))[0];  //randomly choose an empty space in the randomly chosen column
					return position;
				}
			}
			else if (AATools.bestRankCol(playGrid).isEmpty() == true) {  //if there is no column with only 1 card rank
				ArrayList<Integer> junkCols = new ArrayList<Integer>();  //open storage for a list of junk column
				junkCols = AATools.worstColList(playGrid);  //get a list of junk column
				if (junkCols.size() == 1) {  //there is only 1 column of junk card
					position[1] = junkCols.get(0);  //We already know which column this card will be place in, just need to find the right location
					ArrayList<int[]> tmpEmptyRow = AATools.colEmptySpace(playGrid, junkCols.get(0));
					for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
						for (int col = 0; col < SIZE; col++) {  //scan through those rows
							if (playGrid[tmpEmptyRow.get(i)[0]][col] != null && playGrid[tmpEmptyRow.get(i)[0]][col].getSuit() == card.getSuit()) {  //find same suit
								position[0] = tmpEmptyRow.get(i)[0];
								return position;
							}		
						}
					}
					for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
						for (int col = 0; col < SIZE; col++) {  //scan through those rows
							if (playGrid[tmpEmptyRow.get(i)[0]][col] != null && playGrid[tmpEmptyRow.get(i)[0]][col].getRank() == card.getRank()) {  //find same rank
								position[0] = tmpEmptyRow.get(i)[0];
								return position;
							}		
						}
					}
					if (operation == 1) {
						ArrayList<int[]> monteTmpEmptyRow = new ArrayList<int[]>();
						int[] tPosition = {0, position[1]};
						for (int[] row : tmpEmptyRow) {
							tPosition[0] = row[0];
							monteTmpEmptyRow.add(tPosition);
						}
						return position = AAMonteCarlo.MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteTmpEmptyRow, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
					}
					if (operation == 3) {
						position[0] = tmpEmptyRow.get(random.nextInt(tmpEmptyRow.size()))[0]; //TODO check this and following if statement for negative returns
						return position;
					}
				}
				else {  //If there are multiple column tie for worst column
					if (AATools.mostSpaceCol(playGrid, junkCols).size() == 1) {  //if there is only one column that have the largest free space
						position[1] = AATools.mostSpaceCol(playGrid, junkCols).get(0);  //set position to that column
						ArrayList<int[]> tmpEmptyRow = AATools.colEmptySpace(playGrid, position[1]);
						for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
							for (int col = 0; col < SIZE; col++) {  //scan through those rows
								if (playGrid[tmpEmptyRow.get(i)[0]][col] != null && playGrid[tmpEmptyRow.get(i)[0]][col].getSuit() == card.getSuit()) {  //find same suit
									position[0] = tmpEmptyRow.get(i)[0];
									return position;
								}		
							}
						}
						for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
							for (int col = 0; col < SIZE; col++) {  //scan through those rows
								if (playGrid[tmpEmptyRow.get(i)[0]][col] != null && playGrid[tmpEmptyRow.get(i)[0]][col].getRank() == card.getRank()) {  //find same rank
									position[0] = tmpEmptyRow.get(i)[0];
									return position;
								}		
							}
						}
						if (operation == 1) {
							ArrayList<int[]> monteTmpEmptyRow = new ArrayList<int[]>();
							int[] tPosition = {0, position[1]};
							for (int[] row : tmpEmptyRow) {
								tPosition[0] = row[0];
								monteTmpEmptyRow.add(tPosition);
							}
							return position = AAMonteCarlo.MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteTmpEmptyRow, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
						}
						if (operation == 3) {
							position[0] = tmpEmptyRow.get(random.nextInt(tmpEmptyRow.size()))[0];
							return position;
						}
					}
					else if (AATools.mostSpaceCol(playGrid, junkCols).size() > 1) {
						ArrayList<Integer> tmpSpaceCols = new ArrayList<Integer>();
						tmpSpaceCols = AATools.mostSpaceCol(playGrid, junkCols);
						for (int spaceCol : tmpSpaceCols) {
							position[1] = spaceCol;  //set position to that column
							ArrayList<int[]> tmpEmptyRow = AATools.colEmptySpace(playGrid, position[1]);
							for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
								for (int col = 0; col < SIZE; col++) {  //scan through those rows
									if (playGrid[tmpEmptyRow.get(i)[0]][col] != null && playGrid[tmpEmptyRow.get(i)[0]][col].getSuit() == card.getSuit()) {  //find same suit
										position[0] = tmpEmptyRow.get(i)[0];
										return position;
									}		
								}
							}
							for (int i = 0; i < tmpEmptyRow.size(); i++) {  //for each empty row in tmpEmptyRow
								for (int col = 0; col < SIZE; col++) {  //scan through those rows
									if (playGrid[tmpEmptyRow.get(i)[0]][col] != null && playGrid[tmpEmptyRow.get(i)[0]][col].getRank() == card.getRank()) {  //find same rank
										position[0] = tmpEmptyRow.get(i)[0];
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
								for (int[] row : AATools.colEmptySpace(playGrid, col)) {
									tPosition[0] = row[0];
									monteTmpSpaceCols.add(tPosition);
								}
							}
							return position = AAMonteCarlo.MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), monteTmpSpaceCols, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
						}
						if (operation == 3) {
							position[1] = tmpSpaceCols.get(random.nextInt(tmpSpaceCols.size()));
							ArrayList<int[]> tmpEmptyRow = AATools.colEmptySpace(playGrid, position[1]);
							position[0] = tmpEmptyRow.get(random.nextInt(tmpEmptyRow.size()))[0];
							return position;
						} 
					}
					System.out.println("GET OUT");
					System.exit(0);
					if (operation == 1)
						return position = AAMonteCarlo.MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), empty, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
					if (operation == 3)
						return position = AATools.emptySpace(playGrid).get(random.nextInt(AATools.emptySpace(playGrid).size()));
				}
				//TODO possible useless code
				if (operation == 1) {
					ArrayList<int[]> possibleSpaces = new ArrayList<int[]>();
					int[] tPosition = {0, 0};
					for (int col : junkCols) {
						tPosition[1] = col;
						for (int[] row : AATools.colEmptySpace(playGrid, col)) {
							tPosition[0] = row[0];
							possibleSpaces.add(tPosition);
						}
					}
					return position = AAMonteCarlo.MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), possibleSpaces, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
				}
				if (operation == 3) {
					position[1] = junkCols.get(random.nextInt(junkCols.size()));
					position[0] = AATools.colEmptySpace(playGrid, position[1]).get(random.nextInt(AATools.colEmptySpace(playGrid, position[1]).size()))[0];
					return position;
				}
			}
		}
		else {  //if nothing fits
			ArrayList<Integer> possibleFlushRow = new ArrayList<Integer>();
			possibleFlushRow = AATools.possibleFlushRow(playGrid, 1);


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
				return position = AAMonteCarlo.MonteCarlo(playGrid, card, 0, card.getSuit(), card.getRank(), allEmptyLocation, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
			if (operation == 3)
				return position = allEmptyLocation.get(random.nextInt(allEmptyLocation.size()));
		}
		return position;
	}

	public static void main(String[] args) {
	}

}
