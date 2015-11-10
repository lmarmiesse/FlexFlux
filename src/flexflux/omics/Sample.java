package flexflux.omics;

public class Sample {
	
	
	
	
	private String name="";
	private String description="";
	
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
	
	
	
	

}
