from libsbml import *



sbmlns = SBMLNamespaces(3, 1, "qual", 1);
document = SBMLDocument(sbmlns);

#mark qual as required
document.setPackageRequired("qual", True);

model = document.createModel();

c = model.createCompartment()
c.setId("comp")
c.setConstant(True);


#Get a QualModelPlugin object plugged in the model object.
qualPlugin = model.getPlugin("qual");

######################### SPECIES
qs = qualPlugin.createQualitativeSpecies();
qs.setId("a")
qs.setName("a")
qs.setMaxLevel(1)
qs.setCompartment("comp")
qs.setConstant(False)
qs.setInitialLevel(1)

qs = qualPlugin.createQualitativeSpecies();
qs.setId("b")
qs.setName("b")
qs.setMaxLevel(1)
qs.setCompartment("comp")
qs.setConstant(False)
qs.setInitialLevel(0)

qs = qualPlugin.createQualitativeSpecies();
qs.setId("c")
qs.setName("c")
qs.setMaxLevel(1)
qs.setCompartment("comp")
qs.setConstant(False)
qs.setInitialLevel(0)

######################### TRANSITIONS

t = qualPlugin.createTransition()
t.setId("tr_a")
o = t.createOutput();
o.setId(t.getId()+"_out");
o.setQualitativeSpecies("a");
dt = t.createDefaultTerm();
dt.setResultLevel(0);
xn = XMLNode.convertStringToXMLNode('<body xmlns="http://www.w3.org/1999/xhtml"><p>STARTS: 0</p><p>LASTS: 0</p><p>ID: tr_a_default</p></body>')
dt.setNotes(xn)





t = qualPlugin.createTransition()
t.setId("tr_b")
i = t.createInput();
i.setId(t.getId()+"_in_a");
i.setQualitativeSpecies("a");
o = t.createOutput();
o.setId(t.getId()+"_out");
o.setQualitativeSpecies("b");
ft = t.createFunctionTerm();
ft.setMetaId('tr_b_1')
xn = XMLNode.convertStringToXMLNode('<body xmlns="http://www.w3.org/1999/xhtml"><p>STARTS: 1</p><p>LASTS: 0.5</p><p>ID: tr_b_1</p></body>')
ft.setNotes(xn)
#see http://sbml.org/Software/libSBML/docs/java-api/org/sbml/libsbml/libsbml.html#parseL3Formula(java.lang.String)
math = parseL3Formula("a == 1");
ft.setMath(math);
ft.setResultLevel(1)
dt = t.createDefaultTerm();
dt.setResultLevel(0);
xn = XMLNode.convertStringToXMLNode('<body xmlns="http://www.w3.org/1999/xhtml"><p>STARTS: 0</p><p>LASTS: 0</p><p>ID: tr_b_default</p></body>')
dt.setNotes(xn)





t = qualPlugin.createTransition()
t.setId("tr_c")
i = t.createInput();
i.setId(t.getId()+"_in_a");
i.setQualitativeSpecies("a");
i2 = t.createInput();
i2.setId(t.getId()+"_in_b");
i2.setQualitativeSpecies("b");
o = t.createOutput();
o.setId(t.getId()+"_out");
o.setQualitativeSpecies("c");

ft1 = t.createFunctionTerm();
ft1.setMetaId('tr_c_1')
xn = XMLNode.convertStringToXMLNode('<body xmlns="http://www.w3.org/1999/xhtml"><p>STARTS: 0</p><p>LASTS: 5</p><p>ID: tr_c_1</p></body>')
ft1.setNotes(xn)
#see http://sbml.org/Software/libSBML/docs/java-api/org/sbml/libsbml/libsbml.html#parseL3Formula(java.lang.String)
math = parseL3Formula("a == 1 && b ==0");
ft1.setMath(math);
ft1.setResultLevel(1)

ft2 = t.createFunctionTerm();
ft2.setMetaId('tr_c_2')
xn = XMLNode.convertStringToXMLNode('<body xmlns="http://www.w3.org/1999/xhtml"><p>STARTS: 0</p><p>LASTS: 2</p><p>ID: tr_c_2</p></body>')
ft2.setNotes(xn)
#see http://sbml.org/Software/libSBML/docs/java-api/org/sbml/libsbml/libsbml.html#parseL3Formula(java.lang.String)
math = parseL3Formula("a == 0 && b == 1");
ft2.setMath(math);
ft2.setResultLevel(0)

dt = t.createDefaultTerm();
dt.setResultLevel(0);
xn = XMLNode.convertStringToXMLNode('<body xmlns="http://www.w3.org/1999/xhtml"><p>STARTS: 0</p><p>LASTS: 0</p><p>ID: tr_c_default</p></body>')
dt.setNotes(xn)










writeSBML(document, "qual.sbml");
