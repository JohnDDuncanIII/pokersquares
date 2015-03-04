package edu.gettysburg.ai;

import java.util.ArrayList;
import java.util.Random;


public class IanKayPokerSquaresPlayer2 implements PokerSquaresPlayer {
	Card[][] board;
	Random random = new Random();
	Card[] currentDeck;
	int turns;
	@Override
	public void init() {
		board = new Card[5][5];
		for(int row = 0; row < 5; row ++){
			for(int col = 0; col < 5; col ++){
				board[row][col] = null;
			}
		}
		currentDeck = Card.allCards.clone();
		turns = 0;
	}
	
	
	private int mcPlay(Card[][]mcBoard, int mcTurn, Card[] deck){
		Card card = deck[mcTurn++];
		
		int rowPlay = -1;
		int colPlay = -1;
		ArrayList<Integer> colSpaces = new ArrayList<Integer>();
		int suit = card.getSuit();
		for(int col = 0; col < 5; col ++){//check the suit row, get empty columns
			if(mcBoard[suit][col] == null){
				rowPlay = suit;
				colSpaces.add(col);
			}
		}


		for(int row = 4; row >= 0 && rowPlay ==-1; row --){//check additional rows
			for(int col = 0; col < 5; col ++){
				if(mcBoard[row][col] == null){
					rowPlay = row;
					colSpaces.add(col);
				}
			}
		}
		int[] colScores = {1,1,1,1,1};
		for(int col = 0; col < colSpaces.size() && colPlay == -1; col++){//check for pairs
			int colSpace = colSpaces.get(col);
			for(int row = 0; row < 5 && colPlay == -1; row++){
				if(mcBoard[row][colSpace] != null && card.getRank() == mcBoard[row][colSpace].getRank()){
					colPlay = colSpace;
				}
				else if(mcBoard[row][colSpace] != null){
					int currentRank = mcBoard[row][colSpace].getRank();
					for(int row2 = row+1; row2<5; row2++){
						if(mcBoard[row2][colSpace]!= null && currentRank == mcBoard[row2][colSpace].getRank()){
							colScores[colSpace] = 0;
						}
					}
					
				}
				
			}
		}
			
			
		if(colPlay == -1){//look for empty col
			for(int col = 0; col < colSpaces.size(); col++){
				boolean isEmpty = true;
				for(int row = 0; row < 5; row++){
					if(mcBoard[row][colSpaces.get(col)]!= null){
						isEmpty = false;
						break;
					}
				}
				if(isEmpty){
					colPlay = colSpaces.get(col);
					break;
				}
			}
		}
		
		for(int col = 0; col < colSpaces.size() && colPlay == -1; col++){//check for pairless columns
			int colSpace = colSpaces.get(col);
			if(colScores[colSpace] == 1){
				colPlay = colSpace;
			}
		}
		
		if(colPlay == -1){//all else fails, choose randomly
			//PokerSquares.printGrid(mcBoard);
			//System.out.println(mcTurn);
			colPlay = colSpaces.get(random.nextInt(colSpaces.size()));
		}
		mcBoard[rowPlay][colPlay] = card;
		if(mcTurn == 25)
			return PokerSquares.getScore(mcBoard);
		else
			return mcPlay(mcBoard, mcTurn, deck);
	}

	@Override
	public int[] getPlay(Card card, long millisRemaining) {
		for(int i = turns; i< currentDeck.length; i++){
			if(card == currentDeck[i]){
				currentDeck[i] = currentDeck[turns];
				currentDeck[turns] = card;
				}
		}
		
		int rowPlay = -1;
		int colPlay = -1;
		ArrayList<Integer> colSpaces = new ArrayList<Integer>();
		int suit = card.getSuit();
		for(int col = 0; col < 5; col ++){
			if(board[suit][col] == null){
				rowPlay = suit;
				colSpaces.add(col);
			}
		}


		for(int row = 4; row >= 0 && rowPlay ==-1; row --){
			for(int col = 0; col < 5; col ++){
				if(board[row][col] == null){
					rowPlay = row;
					colSpaces.add(col);
				}
			}
		}
		
//		for(int col = 0; col < colSpaces.size(); col++){
//			for(int row = 0; row < 5; row++){
//				if(board[row][colSpaces.get(col)] != null && card.getRank() == board[row][colSpaces.get(col)].getRank()){
//					colPlay = colSpaces.get(col);
//				}
//			}
//		}
//		if(colPlay == -1){
//			for(int col = 0; col < colSpaces.size(); col++){
//				boolean isEmpty = true;
//				for(int row = 0; row < 5; row++){
//					if(board[row][colSpaces.get(col)]!= null){
//						isEmpty = false;
//						break;
//					}
//				}
//				if(isEmpty){
//					colPlay = colSpaces.get(col);
//					break;
//				}
//			}
//		}
//		if(colPlay == -1)
//			colPlay = colSpaces.get(random.nextInt(colSpaces.size()));
		
		if(colSpaces.size()==1)
			colPlay = colSpaces.get(0);
		else{
			int highTotal = 0;
			int highIndex = 0;
			for (int space = 0; space < colSpaces.size(); space++) {
				int trials = 50000;
				int total = 0;
				for (int t = 0; t < trials; t++) {
					Card[][] mcBoard = new Card[5][5];
					for(int x = 0; x <5; x++)//copying board
						for(int y = 0; y <5; y++)
							mcBoard[x][y] = board[x][y];
					mcBoard[rowPlay][colSpaces.get(space)] = card;
					Card[] mcDeck = currentDeck.clone();
					for (int c = turns; c < currentDeck.length; c++) {//shuffle
						int x = random.nextInt(currentDeck.length - turns) + turns;
						Card temp = mcDeck[x];
						mcDeck[x] = mcDeck[c];
						mcDeck[c] = temp;
					}
					total += mcPlay(mcBoard, turns + 1, mcDeck);
				}
				if(total >= highTotal){
					highTotal = total;
					highIndex = space;
				}
			}
			colPlay = colSpaces.get(highIndex);
		}
		
		board[rowPlay][colPlay] = card;
		int[] play = {rowPlay, colPlay};
		turns++;
		return play;
	}

}
