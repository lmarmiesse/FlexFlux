from libsbml import *

import csv


def inverseRule(sign,value):
	
	newValue=value
	newSign=sign
	
	if(sign=="<="):
		newSign=">"
	if(sign=="<"):
		newSign=">="
	if(sign==">="):
		newSign="<"
	if(sign==">"):
		newSign="<="
	if(sign=="="):
		newValue=0
	
	
	return [newSign,newValue]
	
	
	
def getStateFromValue(qualitativeStates,var,value):
	
	if qualitativeStates.has_key(var):
		varQualitativeStates = qualitativeStates[var]
		
	else:
		return value
	
	
	if value=="ND":
		for state in varQualitativeStates:
			if varQualitativeStates[state][0]==value and varQualitativeStates[state][1]==value:
				return state
	
	for state in varQualitativeStates:
		if varQualitativeStates[state][0]==value and varQualitativeStates[state][1]==value:
			return state
			
			
	for state in varQualitativeStates:
		if varQualitativeStates[state][0]<value and varQualitativeStates[state][1]>value:
			return state
			
	print var+" "+str(value)+" none"
	
	
	



variables = {}


with open('Component.csv', 'rb') as csvfile:
	spamreader = csv.reader(csvfile, delimiter='\t')
	
	
	for row in spamreader:
		
		if not row[0] =="ID":
			
			variables[row[0]] = {}
			variables[row[0]]["compartment"]=row[2].replace(" ","_")
			
			variables[row[0]]["SBO"]=row[5]
			
			variables[row[0]]["type"]=row[3]
			variables[row[0]]["name"]=row[1]
			variables[row[0]]["initial"]=row[4].replace(",",".")
			
			variables[row[0]]["varType"]=row[6]
			


sources=[]
targets=[]

simpleRules = []

fi = open("fi","w")
for var in variables:
	fi.write(var+"\t"+variables[var]["initial"]+"\n")
	
fi.close()

with open('RegulatoryNetwork.csv', 'rb') as csvfile:
	spamreader = csv.reader(csvfile, delimiter='\t')
	
	
	for row in spamreader:
		
		if not row[0] =="Regulator ID":
			
			source = row [0]
			target = row [8]
			rule = row [4]
			value = row [5]
			dependency = row [15]
			sign = row [14]
			SBO = row [12]
			
			if not source in sources:
				sources.append(source)
				
			if not target in targets:
				targets.append(target)
			
			
			simpleRule  = {}
			simpleRule["source"] = source
			simpleRule["target"] = target
			simpleRule["rule"] = rule
			simpleRule["value"] = value
			simpleRule["dependency"] = dependency
			simpleRule["sign"] = sign
			simpleRule["SBO"] = SBO
			
			
			simpleRules.append(simpleRule)
			
			#~ print row
			
			

print len(variables)
print len(sources)
print len(targets)
print len(simpleRules)


rules = {}

for simpleRule in simpleRules:
	target = simpleRule["target"]
	source = simpleRule["source"]
	rule = simpleRule["rule"]
	value = simpleRule["value"].replace(",",".")
	dependency = simpleRule["dependency"]
	sign = simpleRule["sign"]
	SBO = simpleRule["SBO"]
	
	if sign=="-":
		a=inverseRule(rule,value)
		rule=a[0]
		simpleRule["rule"]=rule
		value=a[1]
		simpleRule["value"]=value

	
	if not rules.has_key(target):
		rules[target]= []
	
	rules[target].append([source,rule,value,dependency,sign,SBO])



thresholds = {}
for var in variables:
	
	if variables[var]["varType"]=="Boolean":
		continue
	
	
	thresholds[var]=[]
	
	for simpleRule in simpleRules:
		target = simpleRule["target"]
		source = simpleRule["source"]
		value = float(str(simpleRule["value"]).replace(",","."))
		
		if source==var:
			if not value in thresholds[var]:
				thresholds[var].append(value)
				
				
		if target==var:
			if variables[var]["type"]=="Reaction":
				if not 0.0 in thresholds[var]:
					thresholds[var].append(0.0)
			else:
				if not 0.0 in thresholds[var]:
					thresholds[var].append(0.0)
				if not 1.0 in thresholds[var]:
					thresholds[var].append(1.0)



qualitativeStates = {}
for var in thresholds:
	qualitativeStates[var]={}
	
	
	th = thresholds[var]
	th.sort()
	
	if len(th)>0:
		
		i=0
		
		qualitativeStates[var][i]=[-float("inf"),th[0]]
		
		for j in range(len(th)):
			val=th[j]
			i+=1
			qualitativeStates[var][i]=[val,val]
			i+=1
			if len(th)>=j+2:
				qualitativeStates[var][i]=[val,th[j+1]]
			else:
				qualitativeStates[var][i]=[val,float("inf")]
				
				
		i+=1
		if variables[var]["type"]=="Reaction":
			qualitativeStates[var][i]=["ND","ND"]
			
				
		




sbmlns = SBMLNamespaces(3, 1, "qual", 1);
document = SBMLDocument(sbmlns);

#mark qual as required
document.setPackageRequired("qual", True);

model = document.createModel();

c = model.createCompartment()
c.setId("c")
c.setConstant(True);

#Get a QualModelPlugin object plugged in the model object.
qualPlugin = model.getPlugin("qual");

for var in variables:
	qs = qualPlugin.createQualitativeSpecies();
	qs.setId(var);
	qs.setName(variables[var]["name"]);
	qs.setCompartment(variables[var]["compartment"]);
	qs.setSBOTerm(variables[var]["SBO"]);
	qs.setConstant(False);
	
	if qualitativeStates.has_key(var):
		qs.setInitialLevel(int(getStateFromValue(qualitativeStates,var,float(variables[var]["initial"]))))
		qs.setMaxLevel(len(qualitativeStates[var])-1);
	
	else:
		if variables[var]["varType"]=="Boolean":
			qs.setInitialLevel(int(variables[var]["initial"]))
			qs.setMaxLevel(1);
		else:
			print "ERROR : UNKNOWN VARIABLE TYPE : "+variables[var]["varType"]
	
	
	
	if qualitativeStates.has_key(var):
		notesString=""
		for state in qualitativeStates[var]:
			
			lb = qualitativeStates[var][state][0]
			ub = qualitativeStates[var][state][1]
			
			if lb=="ND":
				interval="ND"

			else:
				if (lb!=ub):
				
					strLb = str(lb)
					strUb = str(ub)
					
					if (strLb == "inf"):
						strLb="+inf"
					
					if (strUb == "inf"):
						strUb="+inf"
				
					interval = "]"
					interval+=strLb
					interval+=","
					interval+=strUb
					interval+="["
					
				else:
					strLb = str(lb)
					strUb = str(ub)
					
					if (strLb == "inf"):
						strLb="+inf"
					
					if (strUb == "inf"):
						strUb="+inf"
						
					interval = "["
					interval+=strLb
					interval+=","
					interval+=strUb
					interval+="]"
				
			
			notesString=notesString+"<p>STATE "+str(state)+":"+interval+"</p>"
		
		xn = XMLNode.convertStringToXMLNode('<body xmlns="http://www.w3.org/1999/xhtml">'+notesString+'</body>')
		qs.setNotes(xn)

for target in rules:
	rule = rules[target]
	
	orComponents = []
	andComponents = ""
	
	inputs = []
	
	for component in rule:
		var = component[0]
		sign = component[4]
		SBO = component[5]
		
		if not var in inputs:
			inputs.append([var,sign,SBO])
			
		
		ruleSign = component[1]
		if (ruleSign=="="):
			ruleSign = "=="
				
				
		value = getStateFromValue(qualitativeStates,var,float(component[2]))
		
		
		if component[3] == "OPTIONAL":
			
			orComponents.append(var+ruleSign+str(value))
			
		if component[3] == "EXCLUSIVE":
			
			andComponents+=var+ruleSign+str(value)+" && "
	
	
	andComponents = andComponents[0:-4]
	
	ruleString = ""
	
	if len(orComponents)>0:
	
		for orComponent in orComponents:
			
			if len(andComponents)>0:
			
				ruleString+="("+orComponent+" && "+andComponents+")"+" || "
			
			else:
				ruleString+="("+orComponent+")"+" || "
				
		
		ruleString = ruleString[0:-4]
				
	else:
		ruleString=andComponents

	
	
	
	t = qualPlugin.createTransition();
	t.setId("tr_"+target);
	
	#inputs
	for inp in inputs:
		
		i = t.createInput();
		i.setId(t.getId()+"_in_"+inp[0]);
		i.setQualitativeSpecies(inp[0]);
		i.setSBOTerm(inp[2]);
		
		if inp[1]=="+":
			i.setSign(INPUT_SIGN_POSITIVE)
		elif inp[1]=="-":
			i.setSign(INPUT_SIGN_NEGATIVE)
		
		i.setTransitionEffect(INPUT_TRANSITION_EFFECT_NONE);	
	
	
	#output
	###
	o = t.createOutput();
	o.setId(t.getId()+"_out");
	o.setQualitativeSpecies(target);
	o.setTransitionEffect(OUTPUT_TRANSITION_EFFECT_ASSIGNMENT_LEVEL);
	
	
	####default term
	dt = t.createDefaultTerm();
	
	
	if qualitativeStates.has_key(target):
		
		dt.setResultLevel(int(getStateFromValue(qualitativeStates,target,0.0)))
	else:
		dt.setResultLevel(0)
	
	
	###### if term
	ft = t.createFunctionTerm();
	
	math = parseL3Formula(ruleString);
	
	
	ft.setMath(math);
	
	if qualitativeStates.has_key(target):
		if variables[target]["type"]=="Reaction":
		
			ft.setResultLevel(int(getStateFromValue(qualitativeStates,target,"ND")))

		else:
			ft.setResultLevel(int(getStateFromValue(qualitativeStates,target,1.0)))
			
	else:
		if variables[target]["type"]=="Reaction":
		
			ft.setResultLevel(1)

		else:
			ft.setResultLevel(1)
		
	
	
	
	
writeSBML(document, "RegulatoryNetwork.sbml");









