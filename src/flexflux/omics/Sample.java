package flexflux.omics;

import flexflux.condition.Condition;

public class Sample {
	
	
	
	
	private String name="";
	private String description="";
	
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
	}
	
	public Condition getCondition() {
		return condition;
	}
	
	
	
	
	

}
