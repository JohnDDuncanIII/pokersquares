package edu.gettysburg.ai;

import java.util.ArrayList;


public class WeiAndMattCorrectVersion implements PokerSquaresPlayer {

	private int size = PokerSquares.SIZE;
	private Card[][] simGrid = new Card[size][size]; // simulation game grid
	private boolean boardIsEmpty;
	private ArrayList<Integer> suitArray;
	private ArrayList<Card> cardsPlayed = new ArrayList<Card>();
	private int tempRow;
	private int tempCol;
	private int tempRank;
	private int[] tempRanks = new int[2];
	private int changeT;
	
	@Override
	public void init() { 
		for (int row = 0; row < size; row++){
			for (int col = 0; col < size; col++){
				simGrid[row][col] = null;
			}
		}
		boardIsEmpty = true;
		suitArray = new ArrayList<Integer>();
		for(int i = 0; i < size; i++){
			suitArray.add(-1);
		}
	}
	
	/**
	 * check if a certain row is empty
	 * @param n the index of the row to be checked
	 * @return true if the row is empty otherwise false
	 */
	public boolean isEmptyRow(int n){
		for(int i = 0; i < size; i++){
			if(simGrid[n][i] != null){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * find and return an empty row with the smallest index
	 * @return the index of the row found
	 */
	public int findAnEmptyRow(){
		for(int i = 0; i < size; i++){
			if(isEmptyRow(i)){
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * check if there is an empty row
	 * @return index of the empty row
	 */
	public boolean hasEmptyRow(){
		for(int i = 0; i < size; i++){
			if(isEmptyRow(i)){
				tempRow = i;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * check if there is an empty column
	 * @return index of the empty column
	 */
	public boolean hasEmptyCol(){
		for(int i = 0; i < size; i++){
			if(numCardCol(i) == 0){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * find the number of cards a certain column has
	 * @param n index of the column
	 * @return the number of cards this particular column has
	 */
	public int numCardCol(int n){
		int count = 0;
		for(int i = 0; i < size; i++){
			if(simGrid[i][n] != null){
				count++;
			}
		}
		return count;
	}
	
	/**
	 * find the column that has the least cards and make sure it has an empty spot in the chosen row
	 * @param row the index of the chosen row
	 * @return the index of the column that has the least cards
	 */
	public int findLeastBadCol(int row, Card card){
		int count = 0;
		while(simGrid[row][count] != null)
			count++;
		int compare = numCardCol(count);
		for(int i = count; i < size; i++){
			if((numCardCol(i) < compare && simGrid[row][i] == null && !threeOfKind(i)) || 
					(simGrid[row][i] == null && threeOfKind(count) && threeOfKind(i) && numCardCol(i) <= compare) ||
					(simGrid[row][i] == null && !sameRankCol(i, row, card) && sameRankCol(count, row, card) && numCardCol(i) <= compare)){
				compare = numCardCol(i);
				count = i;
			}
			
		}
		return count;
	}
	
	/**
	 * check if this column has a card of the same rank as the one drawn and the card doesn't appear in the chosen row
	 * @param col column index
	 * @param card the card drawn
	 * @param row the chosen row
	 * @return true or false
	 */
	public boolean sameRankCol(int col, int row, Card card){
		if(simGrid[row][col] == null){
			for(int i = 0; i < size; i++){
				if(simGrid[i][col] != null && simGrid[i][col].getRank() == card.getRank()){
					return true;
				}
			}	
		}	
		return false;
	}
	
	/**
	 * return the column that contains the a card of the same rank as the drawn card
	 * @param card drawn card
	 * @param row providing the parameter for referred method
	 * @return the index of the column
	 */

	/**
	 * check if a specific row is full
	 * @param row index of the row
	 * @return true or false
	 */
	public boolean fullRow(int row){
		for(int i = 0; i < size; i++){
			if(simGrid[row][i] == null){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * find a row that is not full
	 * @return index of the not-full row
	 */
	public int findNotFullRow(){
		int returnRow = -1;
		for(int i = 0; i < size; i++){
			if(!fullRow(i) && (returnRow == -1 || (suitArray.get(i) == -1 && suitArray.get(returnRow) != -1))){
				returnRow = i;
			}
			if(!fullRow(i) && numCardRow(i) < numCardRow(returnRow) && (suitArray.get(i) == -1 || suitArray.get(returnRow) != -1)){
				returnRow = i;
			}
		}
		return returnRow;
	}
	
	/**
	 * check if the column is full
	 * @param col the column to be checked
	 * @return true or false
	 */
	public boolean fullColumn(int col){
		for(int i = 0; i < size; i++){
			if(simGrid[i][col] == null){
				return false;
			}
		}
		return true;
	}
	
	public boolean hasThreeOrMore(int col){
		int count = 0;
		for(int i = 0; i < size; i++){
			if(simGrid[i][col] != null){
				count++;
			}
		}
		if(count >= 3 && !fullColumn(col)){
			return true;
		}
		return false;
	}
	
	public boolean threeOfKind(int col){
		if(hasThreeOrMore(col) && colHasPair(col)){
			for(int i = 0; i < size - 2; i++){
				for(int index = i+1; index < size - 1; index++){
					for(int n = index + 1; n < size; n++){
						if(simGrid[i][col] != null && simGrid[index][col] != null && simGrid[n][col] != null){
							if(simGrid[i][col].getRank() == simGrid[index][col].getRank() && simGrid[i][col].getRank() == simGrid[n][col].getRank()){
								tempRank = simGrid[i][col].getRank();
								return true;
							}		
						}	
					}
				}
			}
		}

		return false;
	}
	
	public boolean canCreateFourOfKind(Card card){
		for(int i = 0; i < size; i++)
			if(!fullColumn(i) && threeOfKind(i) && tempRank == card.getRank()){
				tempCol = i;
				return true;
			}
		return false;
	}
	
	public boolean hasThreeOfKind(){
		for(int i = 0; i < size; i++){
			if(threeOfKind(i)){
				tempCol = i;
				return true;
			}
		}
		return false;
	}
	
	public int findSpotOnCol(int col){
		int returnRow = -1;
		for(int i = 0; i < size; i++){
			if(simGrid[i][col] == null && (returnRow == -1 || suitArray.get(i) == -1)){
				returnRow = i;
			}
		}
		return returnRow;
	}

	public boolean colHasPair(int col){
		for(int i = 0; i < size - 1; i++)
			for(int index = i + 1; index < size; index++)
				if(simGrid[i][col] != null && simGrid[index][col] != null && simGrid[i][col].getRank() == simGrid[index][col].getRank()){
					tempRank = simGrid[i][col].getRank();
					return true;
				}	
		return false;
	}
	
	public boolean colHasFourOfKind(int col){
		for(int i = 0; i < size - 1; i++)
			for(int index = i + 1; index < size; index++)
				if(numCardCol(col) < 4 || (simGrid[i][col] != null && simGrid[index][col] != null && simGrid[i][col].getRank() != simGrid[index][col].getRank()))
					return false;
		return true;
	}
	
	public boolean hasPair(int col, Card card){
		for(int i = 0; i < size - 1; i++)
			for(int index = i + 1; index < size; index++)
				if(simGrid[i][col] != null && simGrid[index][col] != null && simGrid[i][col].getRank() == simGrid[index][col].getRank() && simGrid[suitArray.indexOf(-1)][col] == null && simGrid[i][col].getRank() == card.getRank())
					return true;
		return false;
	}
	
	public boolean addPair(Card card){
		for(int i = 0; i < size; i++)
			if(hasPair(i, card))
				return true;
		return false;
	}
	
	public boolean colHasTwoPairs(int col){
		if(colHasPair(col)){
			int rank = tempRank;
			for(int i = 0; i < size - 1; i++){
				for(int index = i + 1; index < size; index++){
					if(simGrid[i][col] != null && simGrid[index][col] != null && simGrid[i][col].getRank() != rank && simGrid[i][col].getRank() == simGrid[index][col].getRank()){
						tempRanks[0] = rank;
						tempRanks[1] = simGrid[i][col].getRank();
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean hasTwoPairs(){
		for(int i = 0; i < size; i++){
			if(colHasTwoPairs(i) && numCardCol(i) == 4){
				tempCol = i;
				return true;
			}
		}
		return false;
	}

	public boolean canCreateFullHouse(Card card){
		if(hasTwoPairs()){
			if(card.getRank() == tempRanks[0] || card.getRank() == tempRanks[1]){
				return true;
			}
		}
		if(hasThreeOfKind()){
			int col = tempCol;
			if(numCardCol(col) == 4){
				for(int i = 0; i < size; i++){
					if(simGrid[i][col] != null && simGrid[i][col].getRank() == card.getRank()){
						//return true;
						int row = findSpotOnCol(col);
						if(suitArray.get(row) == -1){
							return true;
						}
						else if(suitArray.contains(card.getSuit()) && fullRow(suitArray.indexOf(card.getSuit()))){
							return true;
						}	
								
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * return the number of cards of a row
	 * @param row the index of the chose row
	 * @return number of cards of that row
	 */
	public int numCardRow(int row){
		int n = 0;
		for(int i = 0; i < size; i++)
			if(simGrid[row][i] != null)
				n++;
		return n;
	}
	
	public boolean canMakeChange(int row, Card card){
		int change = 0;
		boolean b = false;
		for(int i = 0; i < size; i++){
			if(simGrid[row][i] == null){
				Card[] hand = new Card[size];
				int oldScore;
				int newScore;
				for(int index = 0; index < size; index++){
					hand[index] = simGrid[index][i];
				}
				oldScore = PokerSquares.getHandScore(hand);
				hand[row] = card;
				newScore = PokerSquares.getHandScore(hand);
				if(newScore - oldScore > change){
					change = newScore - oldScore;
					changeT = change;
					tempCol = i;
					b = true;
				}
			}
		}
		return b;
	}
	
	@Override
	public int[] getPlay(Card card, long millisRemaining) {
		//System.out.println(card);
		cardsPlayed.add(card);
		int row;
		int col;
		if(boardIsEmpty){
			int[] playPos = {0, 0};
			suitArray.set(0, card.getSuit());
			simGrid[0][0] = card;
			boardIsEmpty = false;
			return playPos;
		}
		
		if(canCreateFourOfKind(card)){
			col = tempCol;
			if(suitArray.contains(card.getSuit()) && simGrid[suitArray.indexOf(card.getSuit())][col] == null){
				row = suitArray.indexOf(card.getSuit());
			}
			else if(!suitArray.contains(card.getSuit()) && hasEmptyRow()){
				row = tempRow;
				suitArray.set(row, card.getSuit());
			}
			else{
				row = findSpotOnCol(col);
				if(card.getSuit() != suitArray.get(row)){
					suitArray.set(row, -1);
				}
			}
			int[] playPos = {row, col};
			simGrid[row][col] = card;
			return playPos;
		}
		
		if(canCreateFullHouse(card)){
			col = tempCol;
			row = findSpotOnCol(col);
			if(isEmptyRow(row) && !suitArray.contains(card.getSuit())){
				suitArray.set(row, card.getSuit());
			}
			if(card.getSuit() != suitArray.get(row)){
				suitArray.set(row, -1);
			}
			int[] playPos = {row, col};
			simGrid[row][col] = card;
			return playPos;
		}
		
		if(!suitArray.contains(card.getSuit())){
			if(hasEmptyRow()){
				row = tempRow;
				suitArray.set(row, card.getSuit());
				if(canMakeChange(row, card)){
					col = tempCol;
				}
				else{
					col = findLeastBadCol(row, card);
				}
				int[] playPos = {row, col};
				simGrid[row][col] = card;
				return playPos;			
				
			}
			else{
				row = findNotFullRow();
				if(canMakeChange(row, card)){
					col = tempCol;
				}
				else{
					col = findLeastBadCol(row, card);
				}
				if(card.getSuit() != suitArray.get(row)){
					suitArray.set(row, -1);
				}
				int[] playPos = {row, col};
				simGrid[row][col] = card;
				return playPos;
			}		
		}
		else{
			if(!fullRow(suitArray.indexOf(card.getSuit()))){
                row = suitArray.indexOf(card.getSuit());
                if(canMakeChange(row, card)){
					col = tempCol;
				}
				else if(canMakeChange(suitArray.indexOf(-1), card) && (changeT > 5 || numCardRow(suitArray.indexOf(-1)) < 2)){
					col = tempCol;
					row = suitArray.indexOf(-1);
				}
				else{
					col = findLeastBadCol(row, card);
				}
				int[] playPos = {row, col};
				simGrid[row][col] = card;
				return playPos;
			}
			else{
				row = findNotFullRow();
				if(canMakeChange(row, card)){
					col = tempCol;
				}
				
				else{
					col = findLeastBadCol(row, card);
				}
				if(card.getSuit() != suitArray.get(row)){
					suitArray.set(row, -1);
				}
				int[] playPos = {row, col};
				simGrid[row][col] = card;
				return playPos;
				
			}
		}
		
	}
}


