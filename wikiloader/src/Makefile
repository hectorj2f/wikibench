CC = gcc
CFLAGS = -Wall -O3 -march=native -g
#CFLAGS = -Wall -O2
#CFLAGS2 = -Wall  -g
#CFLAGS = -Wall -ansi -O2
#CFLAGS2 = -Wall -ansi -g
INCL = -I. -I..
LIBS = -lexpat

all: wikiloader

wikiloader: wikiloader.o 
	$(CC) $(CFLAGS) wikiloader.o linkedlist_int.o $(INCL) $(LIBS) -o $@ 

linkedlist_: linkedlist_int.c linkedlist_int.h	
	$(CC) -c linkedlist_int.c $(INCL) $(CFLAGS)

	touch $@

wikiloader.o: wikiloader.c wikiloader.h linkedlist_
	$(CC) -c wikiloader.c $(INCL) $(CFLAGS)

clean:
	rm *.o linkedlist_ wikiloader
