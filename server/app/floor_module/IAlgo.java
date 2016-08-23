package floor_module;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;

public interface IAlgo {
	
	public void proccess(ArrayList<JsonNode> bucket, String floor);
	
	public String getFloor();
}
