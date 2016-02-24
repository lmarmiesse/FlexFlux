package flexflux.omics;

import flexflux.condition.Condition;

public class Sample {
	
	
	
	
	private String name="";
	private String description="";
	private boolean hasCondition=false;
	
	private Condition condition;
	
	public Sample(String name){
		this.name=name;
	}
	
	public Sample(String name,String description){
		this.name=name;
		this.description=description;
	}

	public String getName() {
		return name;
	}

	public void setCondition(Condition cond) {
		this.condition=cond;
		hasCondition=true;
	}
	
	public boolean getHasCondtition(){
		return hasCondition;
	}
	
	public Condition getCondition() {
		return condition;
	}
	
	
	
	
	

}
