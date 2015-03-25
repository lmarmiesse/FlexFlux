EXTRA_DIST = \
glpk-java.tex \
index.sty \
libglpk-java.3 \
mybib.bib \
swimlanes.eps \
swimlanes.graphml \
glpk-java.pdf

all:
	gzip -c libglpk-java.3 > libglpk-java.3.gz

clean-local:
	rm -f *.aux
	rm -f *.bbl
	rm -f *.blg
	rm -f *.gz
	rm -f *.idx
	rm -f *.ilg
	rm -f *.ind
	rm -f *.log
	rm -f *.out
	rm -f *.toc
	rm -f *~

documentation:
	epstopdf swimlanes.eps
	pdflatex glpk-java.tex
	bibtex glpk-java
	pdflatex glpk-java.tex
	makeindex glpk-java.idx
	pdflatex glpk-java.tex

install:
	mkdir -p -m 755 $(DESTDIR)${docdir};true
	install -m 644 glpk-java.pdf $(DESTDIR)${docdir}/glpk-java.pdf
	mkdir -p -m 755 $(DESTDIR)${mandir}/man3/;true
	install -m 644 libglpk-java.3.gz $(DESTDIR)${mandir}/man3/libglpk-java.3.gz

check:
	if test ! -f glpk-java.pdf;then echo glpk-java.pdf not found;exit 1; fi
