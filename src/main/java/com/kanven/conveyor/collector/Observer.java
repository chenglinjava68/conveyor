package com.kanven.conveyor.collector;

public interface Observer<T> {

	void observe(T record);

}
