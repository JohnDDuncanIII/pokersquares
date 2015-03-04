package edu.gettysburg.ai;

import java.util.ArrayList;


public class Player_WeiAndMatt implements PokerSquaresPlayer {
	
	private int size = PokerSquares.SIZE;
	private Card[][] simGrid = new Card[size][size]; // simulation game grid
	private boolean boardIsEmpty;
	private ArrayList<Integer> suitArray;
	
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
	public boolean checkEmptyRow(int n){
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
			if(checkEmptyRow(i)){
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
			if(checkEmptyRow(i)){
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
	public int numCardsCol(int n){
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
	public int findLeastCol(int row){
		int n = 0;
		while(simGrid[row][n] != null){
			n++;
		}
		int count = n;
		int compare = numCardsCol(n);
		for(int i = n; i < size; i++){
			if(numCardsCol(i) < compare && simGrid[row][i] == null){
				compare = numCardsCol(i);
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
		for(int i = 0; i < size; i++){
			if(simGrid[i][col] != null && i != row && simGrid[row][col] == null){
				if(simGrid[i][col].getRank() == card.getRank()){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * check if there exists a column that has a card of the same rank as the drawn one
	 * @param card
	 * @param row
	 * @return
	 */
	public boolean existsSameRankCol(Card card, int row){
		for(int i = 0; i < size; i++){
			if(sameRankCol(i, row, card)){
				return true;
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
	public int returnSameCol(Card card, int row){
		int index = -1;
		for(int i = 0; i < size; i++){
			if(sameRankCol(i, row, card)){
				index = i;
			}
		}
		return index;
	}
	
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
		for(int i = 0; i < size; i++){
			if(!fullRow(i)){
				return i;
			}
		}
		return -1;
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
	
	@Override
	public int[] getPlay(Card card, long millisRemaining) {
		//System.out.println(card);
		int row;
		int col;
		if(boardIsEmpty){
			int[] playPos = {0, 0};
			suitArray.set(0, card.getSuit());
			simGrid[0][0] = card;
			boardIsEmpty = false;
			return playPos;
		}

		if(!suitArray.contains(card.getSuit())){
			if(hasEmptyRow()){
				row = findAnEmptyRow();
				suitArray.set(row, card.getSuit());
				if(existsSameRankCol(card, row)){
					col = returnSameCol(card, row);
				}
				else{
					col = findLeastCol(row);
				}
				int[] playPos = {row, col};
				simGrid[row][col] = card;
				return playPos;
			}
			else{
				row = findNotFullRow();
				if(existsSameRankCol(card, row)){
					col = returnSameCol(card, row);
				}
				else{
					col = findLeastCol(row);
				}
				int[] playPos = {row, col};
				simGrid[row][col] = card;
				return playPos;
			}		
		}
		if(suitArray.contains(card.getSuit())){
			if(!fullRow(suitArray.indexOf(card.getSuit()))){
                row = suitArray.indexOf(card.getSuit());
				if(existsSameRankCol(card, row)){
					col = returnSameCol(card, row);
				}
				else{
					col = findLeastCol(row);
				}
				int[] playPos = {row, col};
				simGrid[row][col] = card;
				return playPos;
			}
			else{
				if(!fullRow(suitArray.indexOf(-1))){
					row = suitArray.indexOf(-1);
					if(existsSameRankCol(card, row)){
						col = returnSameCol(card, row);
					}
					else{
						col = findLeastCol(row);
					}
					int[] playPos = {row, col};
					simGrid[row][col] = card;
					return playPos;
				}
				else{
					row = findNotFullRow();
					if(existsSameRankCol(card, row)){
						col = returnSameCol(card, row);
					}
					else{
						col = findLeastCol(row);
					}
					int[] playPos = {row, col};
					simGrid[row][col] = card;
					return playPos;
				}
			}
		}
		
		return null;
	}
	
	public static void main(String[] args){
		Player_WeiAndEleni player = new Player_WeiAndEleni();
		new PokerSquares(player, 60000).play();
	}

}
