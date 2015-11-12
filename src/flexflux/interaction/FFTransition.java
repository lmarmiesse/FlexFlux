package flexflux.interaction;
import java.util.ArrayList;
import java.util.List;

import flexflux.interaction.Interaction;
import flexflux.interaction.Unique;


public class FFTransition {
	
	
	
	private List<Interaction> conditionalInteractions= new ArrayList<Interaction>();
	private Interaction defaultInteraction;
	
	public FFTransition(){
		
		
		
	}
	
	public void setdefaultInteraction(Interaction defaultInt){
		defaultInteraction=defaultInt;
	}
	
	public Interaction getdefaultInteraction(){
		return defaultInteraction;
	}
	
	public void addConditionalInteraction(Interaction inter){
		conditionalInteractions.add(inter);
	}
	
	public List<Interaction> getConditionalInteractions(){
		return conditionalInteractions;
	}
	
	

}
