package edu.gettysburg.ai;

import java.util.ArrayList;
import java.util.Random;


public class AATools {
	public static Random random = new Random();
	public static ArrayList<int[]> empty = new ArrayList<int[]>();
	private final static int SIZE = PokerSquares.SIZE;
	public static String[] rNames = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K"};
	public static String[] sNames = {"C", "D", "H", "S"};

	public static boolean isOutOfTime (long millisRemaining) {  //Check current run time
		AAFinalPokerSquarePlayer.currentTime = System.currentTimeMillis();
		AAFinalPokerSquarePlayer.millisRem = System.currentTimeMillis() - AAFinalPokerSquarePlayer.startTime;
		if (AAFinalPokerSquarePlayer.millisRem > 60000 - AAFinalPokerSquarePlayer.timeCutOff) {
			return true;
		}
		return false;
	}

	public static ArrayList<int[]> rowEmptySpace(Card[][] cardGridThis, int row){  //find empty space in playGrid for a given row
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

	public static int[] rankColCheck(Card card, Card[][] cardGrid, ArrayList<int[]> goodList, int operation, long millisRemaining){ //Operation"1" uses algo WITH MonteCarlo, Operation"2" use simple algo WITHOUT MonteCarlo
		int[] position = {0, 0};

		for (int i = 0; i < goodList.size(); i++){
			int[] currentLocation = goodList.get(i);
			for (int r = 0; r < SIZE; r++){
				if (cardGrid[r][currentLocation[1]] != null) {
					if (cardGrid[r][currentLocation[1]].getRank() == card.getRank()){ //Find Same rank cards to pair with
						return position = currentLocation;
					}
				}
				else if (!emptyCol(cardGrid).isEmpty()) {  //Find empty column to start a new rank
					position[1] = emptyCol(cardGrid).get(0);
					ArrayList<int[]> sameSuit = new ArrayList<int[]>();
					for (int row : pureSuit(cardGrid, card)) {
						if (cardGrid[row][position[1]] == null) {
							int[] tmp = {row, position[1]};
							sameSuit.add(tmp);
						}
					}
					if (sameSuit.isEmpty()) {
						for (int[] emptySpace : colEmptySpace(cardGrid, position[1]))
							sameSuit.add(emptySpace);
					}
					if (sameSuit.size() == 1)
						return position = sameSuit.get(0);
					if (sameSuit.size() > 1) {
						if (operation == 1)
							return position = AAMonteCarlo.MonteCarlo(cardGrid, card, -1, -1, -1, sameSuit, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
						if (operation == 3)
							return position = sameSuit.get(random.nextInt(sameSuit.size()));
					}
				}
				else {
					if (operation == 1)
						return position = AAMonteCarlo.MonteCarlo(cardGrid, card, 1, 4, card.getRank(), empty, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
					if (operation == 3)
						return position = AAFullHouse.fullHousePriority(card, cardGrid, operation, millisRemaining);  //Find location for junk cards TODO
				}
			}
		}

		return position = AAMonteCarlo.MonteCarlo(cardGrid, card, 0, -1, -1, empty, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
	}

	public static int countGridEmptySpace(Card[][] grid) {  //Count amount of empty space in a grid
		int count = 0;
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				if (grid[row][col] == null)
					count++;
		return count;
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

	public static boolean checkFullGrid(Card[][] grid, int amount) {  //Check  any 5x5 grid if it empty
		int count = 0;
		for (int row = 0; row < SIZE; row++)  //Check for empty space
			for (int col = 0; col < SIZE; col++)
				if (grid[row][col] != null)
					count++;
		if (count >= amount)
			return true;
		else
			return false;
	}

	public static ArrayList<int[]> sameRank (Card[][] grid, Card card) {  //Find cards with the same rank in the grid then return the position of that card
		ArrayList<int[]> sameRank = new ArrayList<int[]>();
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				if (grid[row][col] != null && card.getRank() == grid[row][col].getRank()) {
					int[] tmp = {row, col};
					sameRank.add(tmp);
				}

		return sameRank;
	}

	public static ArrayList<int[]> colEmptySpace (Card[][] grid, int col) {  //Input a column and find all empty space in that column
		ArrayList<int[]> arrayList = new ArrayList<int[]>();
		for (int row = 0; row < SIZE; row++) {
			if (grid[row][col] == null) {
				int[] position = {row, col};
				arrayList.add(position);;
			}
		}
		return arrayList;
	}

	//	public static int[] findEmptyCol(Card[][] cardGridThis, ArrayList<int[]> arrayList){  //Find and return an empty column by using an arrayList of possible locations
	//		for (int e = 0; e < arrayList.size(); e++){
	//			int count = 0;
	//			int[] tmpLocation = arrayList.get(e);
	//			for (int w = 0; w < SIZE; w++)
	//				if (cardGridThis[w][tmpLocation[1]] == null)
	//					count = count + 1;
	//			if (count == SIZE)
	//				return tmpLocation;
	//		}
	//		return null;
	//	}

	public static int[] findFreeCol(Card[][] cardGrid, ArrayList<int[]> arrayList, Card card, int operation, long millisRemaining) {  //Find and return column with most free space using an arrayList of possible locations
		int[] spaceAmount = new int[arrayList.size()];
		int[] tmpLocation = arrayList.get(0);
		if (arrayList.size() == 1)
			return tmpLocation;
		else{
			if (operation == 1)
				tmpLocation = AAMonteCarlo.MonteCarlo(cardGrid, card, 0, card.getSuit(), card.getRank(), arrayList, millisRemaining, AAFinalPokerSquarePlayer.trialsPerEmptySpace);
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

	public static int sameSuit (Card[][] grid, ArrayList<int[]> emptySpace, Card card) {  //return the row number
		for (int i = 0; i < emptySpace.size(); i++) {  //for every item in the emptySpace list...
			int[] itemTmp = emptySpace.get(i);
			for (int col = 0; col < SIZE; col++)  //Scan through all place in the same row as the item
				if (grid[itemTmp[0]][col] != null && grid[itemTmp[0]][col].getSuit() == card.getSuit())  //If there is a card
					return itemTmp[0];  //Return the item
		}
		return -1;
	}



	public static ArrayList<Integer> emptyRow (Card[][] grid) {  //find completely empty row
		int count = 0;
		ArrayList<Integer> emptyRow = new ArrayList<Integer>();
		for (int row = 0; row < SIZE; row++) {
			count = 0;
			for (int col = 0; col < SIZE; col++)
				if (grid[row][col] == null)
					count++;
			if (count == SIZE)  //if there are 5 empty space in a column, return that column
				emptyRow.add(row);
		}
		return emptyRow;  //return -1 if no empty row
	}

	public static ArrayList<Integer> emptyCol (Card[][] grid) {  //find completely empty column
		ArrayList<Integer> emptyCol = new ArrayList<Integer>();
		int count = 0;
		for (int col = 0; col < SIZE; col++) {
			count = 0;
			for (int row = 0; row < SIZE; row++) 
				if (grid[row][col] == null)
					count++;
			if (count == SIZE)  //if there are 5 empty space in a column, return that column
				emptyCol.add(col);
		}
		return emptyCol;  //return -1 if no empty col
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

	public static ArrayList<Integer> worstColList (Card[][] grid) {
		ArrayList<Integer> cols = new ArrayList<Integer>();
		int amount = worstColAmount(grid);
		ArrayList<Integer> ranks = new ArrayList<Integer>();
		for (int col = 0; col < SIZE; col++) {
			ranks.clear();
			for (int row = 0; row < SIZE; row++)  //go through all places in a column
				if (grid[row][col] != null)  //when ever you find a card in a particular column
					if (ranks.contains(grid[row][col].getRank()) == false)  //if a rank have not been recorded yet
						ranks.add(grid[row][col].getRank());  //record the rank
			if (ranks.size() == amount && isFull(grid, col, -1) == false) //return the column that occupied by only one rank
				cols.add(col);
		}		
		return cols;
	}

	public static boolean isFull (Card[][] grid, int loco, int rowORcol) {  //1 means row, -1 means col, loco store either the row or col being tested
		if (loco >= SIZE )
			return true;
		int count = 0;
		if (rowORcol == 1)
			for (int col = 0; col < SIZE; col++)
				if (grid[loco][col] != null)
					count++;
		if (rowORcol == -1)
			for (int row = 0; row < SIZE; row++)
				if (grid[row][loco] != null)
					count++;
		if (count == SIZE)
			return true;
		else
			return false;
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
			for (int row = 0; row < SIZE; row++)  //go through all places in a column
				if (grid[row][col] != null)  //when ever you find a card in a particular column
					if (ranks.contains(grid[row][col].getRank()) == false)  //if a rank have not been recorded yet
						ranks.add(grid[row][col].getRank());  //record the rank
			if (ranks.size() > amount) //return the column that occupied by only one rank
				amount = ranks.size();
		}
		return amount;
	}

	public static ArrayList<Integer> mostSpaceCol (Card[][] grid, ArrayList<Integer> columnList) {
		ArrayList<Integer> mostSpaceCol = new ArrayList<Integer>();
		int count = 0;
		int highestCount = 1;
		for (int col : columnList) {
			count = 0;
			for (int row = 0; row < SIZE; row++)
				if (grid[row][col] == null)
					count++;
			if (count == highestCount)
				mostSpaceCol.add(col);
			else if (count > highestCount) {
				highestCount = count;
				mostSpaceCol.clear();
				mostSpaceCol.add(col);
			}
		}
		return mostSpaceCol;
	}

	public static ArrayList<int[]> emptySpace (Card[][] grid) {  //Find all empty Space
		ArrayList<int[]> emptySpace = new ArrayList<int[]>();
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				if (grid[row][col] == null) {
					int[] position = {row, col};
					emptySpace.add(position);
				}
		return emptySpace;
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

	public static ArrayList<Integer> pureSuit (Card[][] grid, Card card) {  //return list of row that have only the same suit (NO MIX) as the card and have at least 1 free space
		ArrayList<Integer> rows = new ArrayList<Integer>();
		for (int row = 0; row < SIZE; row++) {
			boolean isGood = true;
			int count = 0;
			for (int col = 0; col < SIZE; col++) {  //scan through all item in a row
				if (grid[row][col] != null && grid[row][col].getSuit() != card.getSuit())  //if an item is not the same suit, mark it as bad suit
					isGood = false;
				if (grid[row][col] == null)  //count the amount of empty space
					count++;
			}
			if (isGood && count > 0 && count != 5)  //if the row is good and have at least 1 empty space, add it to the list
				rows.add(row);
		}
		return rows;  //return list of good rows
	}

	public static String cardNames(int rank, int suit){ //convert getRank, getSuit into string.
		String cardName = rNames[rank] + sNames[suit];
		return cardName;
	}

	public static boolean suitExist(Card[][] grid, Card card) {
		int cardSuit = card.getSuit();
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)  //scan through all item in a row
				if (grid[row][col] != null && grid[row][col].getSuit() == cardSuit)  //if an item is not the same suit, mark it as bad suit
					return true;
		return false;
	}

	public static boolean rankExist(Card[][] grid, Card card) {
		int cardRank = card.getRank();
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)  //scan through all item in a row
				if (grid[row][col] != null && grid[row][col].getRank() == cardRank)  //if an item is not the same suit, mark it as bad suit
					return true;
		return false;
	}

	public static boolean moreRank (Card[][] grid, boolean rowOrCol, int useRow, int useCol) {
		ArrayList<Integer> rank = new ArrayList<Integer>();
		if (rowOrCol == true) {
			for (int col = 0; col < SIZE; col++) {
				if (grid[useRow][col] != null)
					if (!rank.contains(grid[useRow][col].getRank()))
						rank.add(grid[useRow][col].getRank());
			}
			if (rank.size() > 2)
				return false;
			else
				return true;
		}
		if (rowOrCol == false) {
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][useCol] != null)
					if (!rank.contains(grid[row][useCol].getRank()))
						rank.add(grid[row][useCol].getRank());
			}
			if (rank.size() > 2)
				return false;
			else
				return true;
		}
		return true;
	}
	
	public static int[] possibleFour(Card[][] grid, Card card) {  //Return a location if a given card can complete a column of fullHouse or FourOfAKind
		for (int col = 0; col < SIZE; col++) {
			int emptySpace = 0;
			boolean sameCard = false;
			int goodRow = -1;
			ArrayList<Integer> ranks = new ArrayList<Integer>();
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][col] == null) {
					emptySpace++;
					goodRow = row;
				}
				if (grid[row][col] != null && grid[row][col].getRank() == card.getRank()) {
					sameCard = true;
				}
				if (grid[row][col] != null && ranks.contains(grid[row][col].getRank()) == false) {
					ranks.add(grid[row][col].getRank());
				}
			}
			if (goodRow != -1 && emptySpace == 1 && sameCard && ranks.size() == 2) {
				int[] position = {goodRow, col};
				return position;
			}
		}
		int[] position = {-1, -1};
		return position;
	}

	public static ArrayList<int[]> mostRank (Card[][] grid) {
		ArrayList<int[]> mostRank = new ArrayList<int[]>();
		ArrayList<Integer> ranks = new ArrayList<Integer>();
		int[] rows = new int[SIZE];
		int[] cols = new int[SIZE];
		for (int row = 0; row < SIZE; row++) {
			ranks.clear();
			for (int col = 0; col < SIZE; col++) {
				if (grid[row][col] != null && !ranks.contains(grid[row][col].getRank()))
					ranks.add(grid[row][col].getRank());
			}
			rows[row] = ranks.size();
		}
		for (int col = 0; col < SIZE; col++) {
			ranks.clear();
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][col] != null && !ranks.contains(grid[row][col].getRank()))
					ranks.add(grid[row][col].getRank());
			}
			cols[col] = ranks.size();
		}
		int highest = 0;
		for (int i = 0; i < 3; i++) {
			for (int row = 0; row < rows.length; row++) {
				if (rows[row] > highest)
					highest = rows[row];
				if (rows[row] < highest)
					rows[row] = 0;
			}
		}
		highest = 0;
		for (int i = 0; i < 3; i++) {
			for (int col = 0; col < rows.length; col++) {
				if (cols[col] > highest)
					highest = rows[col];
				if (cols[col] < highest)
					cols[col] = 0;
			}
		}
		
		for (int i = 0; i < SIZE; i++) {
			if (cols[i] != 0 && rows[i] != 0) {
				int[] tmp = {i, i};
				mostRank.add(tmp);
			}
				
		}


		return mostRank;
	}

	public static void main(String[] args) {

	}

}
