package edu.gettysburg.ai;

import java.util.ArrayList;
import java.util.Stack;


/**
 * DancingLinkedListNode - Doubly linked list node according to Knuth's dancing links pattern that allows 
 * stack-based deletions and efficient reinsertions in reverse order.  See main test code below for example.
 * @author Todd Neller
 *
 * @param <E>
 */
public class DancingLinkedListNode<E> {
	public E data;
	public DancingLinkedListNode<E> prev = this, next = this;

	public DancingLinkedListNode() {
	}

	public DancingLinkedListNode(E data, DancingLinkedListNode<E> prev, DancingLinkedListNode<E> next) {
		this.data = data;
		this.prev = prev;
		this.next = next;
	}

	public DancingLinkedListNode<E> removeSelf() {
		if (prev != this) {
			next.prev = prev;
			prev.next = next;
		}
		return this;
	}

	public void reinsertSelf() {
		next.prev = this;
		prev.next = this;
	}

	public DancingLinkedListNode<E> insertNext(E data) {
		DancingLinkedListNode<E> newNode = new DancingLinkedListNode<E>(data, this, this.next);
		next.prev = newNode;
		next = newNode;
		return newNode;
	}
	
	public DancingLinkedListNode<E> insertPrev(E data) {
		DancingLinkedListNode<E> newNode = new DancingLinkedListNode<E>(data, this.prev, this);
		prev.next = newNode;
		prev = newNode;
		return newNode;
	}
	
	/**
	 * Call on dummy head/tail node of circular doubly linked list (data == null) to return contents of list.
	 * @return contents of list
	 */
	public ArrayList<E> getArrayList() {
		ArrayList<E> list = new ArrayList<E>();
		DancingLinkedListNode<E> current = next;
		while (current != this) {
			list.add(current.data);
			current = current.next;
		}
		return list;
	}

	public static void main(String[] args) {
		DancingLinkedListNode<Integer> list = new DancingLinkedListNode<Integer>();
		DancingLinkedListNode<Integer> one = list.insertPrev(1);
		DancingLinkedListNode<Integer> two = list.insertPrev(2);
		DancingLinkedListNode<Integer> three = list.insertPrev(3);
		Stack<DancingLinkedListNode<Integer>> stack = new Stack<DancingLinkedListNode<Integer>>();
		System.out.println(list.getArrayList());
		stack.push(two.removeSelf());
		System.out.println(list.getArrayList());
		stack.push(three.removeSelf());
		System.out.println(list.getArrayList());
		stack.push(one.removeSelf());
		System.out.println(list.getArrayList());
		while (!stack.isEmpty()) {
			stack.pop().reinsertSelf();
			System.out.println(list.getArrayList());
		}
	}

}
