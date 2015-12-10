package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;

public interface Model {
	
	public void writeTo(DataOutputStream os) throws IOException;

}
