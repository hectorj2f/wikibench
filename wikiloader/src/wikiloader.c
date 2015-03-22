//	Copyright (c) 2007, Guido Urdaneta  (guidox@gmail.com)
//  Andreea Marin (andreea.marin@gmail.com)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include <stdio.h>
#include <expat.h>
#include <string.h>
#include <ctype.h>
#include <stdlib.h>
#include <time.h>
#include "linkedlist_int.h"
#include "wikiloader.h"

#ifdef XML_LARGE_SIZE
#if defined(XML_USE_MSC_EXTENSIONS) && _MSC_VER < 1400
#define XML_FMT_INT_MOD "I64"
#else
#define XML_FMT_INT_MOD "ll"
#endif
#else
#define XML_FMT_INT_MOD "l"
#endif

#define FILE_BUF_SIZE 20000
#define DATA_BUF_SIZE 20000
#define MIN(a,b) (((a)<(b))?(a):(b))

enum _Status {WP_PAGE, WP_PAGE_TITLE, WP_PAGE_ID,
		WP_REV, WP_REV_ID, WP_REV_TS, 
		WP_REV_CONT, WP_REV_CONT_USERNAME, WP_REV_CONT_ID, WP_REV_CONT_IP,
		WP_REV_MINOR, WP_REV_COMMENT,
		WP_TEXT,
		WP_OTHER};
typedef enum _Status Status;

static GULinkedListInt *status_stack = NULL;
static char file_buf[FILE_BUF_SIZE];
static char data_buf[DATA_BUF_SIZE];
static int copy_count;
static int page_count = 0;
static int rev_count = 0;
static int text_count = 0;

//command line parameters
static int disable_indexing = 0;
static int enable_logging = 0;
static int delete_data = 0;
static int is_restart = 0;
static int restart_page_id = -1;
static int page_interval = -1;
static char log_file_path[40];

static FILE *log_file;

static void XMLCALL wp_elem_start_handler(void *data, const char *el, const char **attr);
static void XMLCALL wp_elem_end_handler(void *data, const char *el);
static void XMLCALL wp_data_handler(void *userData, const XML_Char *s, int len);
static void buf_strcpy(char *dest, size_t len);
static void buf_tscpy(char *dest);
static void reset_page(Page *page);
static void reset_revision(Revision *rev);
static void reset_text(Text *text);
static int empty_text(char *ptr);
static int page_is_redirect(char text[TEXT_LEN]);
static int utf8_length(char *ptr);
static void convert_title_to_wiki_format(char * s, int len);

static void XMLCALL wp_elem_start_handler(void *data, const char *el, const char **attr)
{
if (!strcmp(el, "page")) {
	gull_int_add_front(status_stack, WP_PAGE);
}  
else if (!strcmp(el, "revision")) {
	gull_int_add_front(status_stack, WP_REV);
}  
else if(!strcmp(el, "text")) {
	gull_int_add_front(status_stack, WP_TEXT);
} 
else {
	gull_int_add_front(status_stack, WP_OTHER);
}
}

static void XMLCALL wp_elem_end_handler(void *data, const char *el)
{
int status;

gull_int_remove_front(status_stack, &status);

	switch(status) {
		case WP_PAGE:	  
			page_count++;
			if (page_count % 1000 == 0)
				fprintf(stderr, "%d pages processed, %d revisions processed, %d text processed\n", page_count, rev_count, text_count);
			
		break;
		
		case WP_REV:   			
			rev_count++;      
		break;
		
		
		case WP_TEXT:
			text_count++;
		break;    
	}

/* data is ready, do something with it */
copy_count = 0;
data_buf[0] = 0;
}

static void XMLCALL wp_data_handler(void *userData, const XML_Char *s, int len)
{
int i=0;
if (data_buf[0] == 0) {
	for (i=0; i<len;i++) {
		if (!isspace(s[i])) 
			break;
	}
}

if (status_stack->head->data != WP_OTHER)
{
	int true_len = len-i;
	int available = DATA_BUF_SIZE - copy_count - 1;
	int to_copy = MIN(true_len, available);
	strncat(data_buf, s+i, to_copy);
	copy_count += to_copy;
	data_buf[copy_count] = 0;
}

}

int main(int argc, char *argv[])
{

status_stack = gull_int_new(NULL);

/* Checking the parameters of the wikiloader
* Usage:
*	wikiloader -d 			; for deleting information from the tables: page, revision, text
*	wikiloader -i 			; for turning indexing off before inserting data into the tables, and 		turning it on when finishing the whole process of dumping the data
*	wikiloader -l filepath page_interval	; for turning on logging, the filepath of the log file, and the page_interval showing between how many page counts the page_id is written into the log file
*   wikiloader -r page_id	; restart page insertion after fail from the page_id
*	wikiloader 				; default
*/

if (argc == 1)
{
/*
* Default parameters:
* - no deleting
* - no removing indexes
* - no logging
* - no restart
*/
}

else if (argc > 8)
{
	fprintf(stderr, "The number of parameters is incorrect. Please review your command.\n");
	return 0;
}
else
{
	int i;
	for (i = 1; i < argc; i++)
	{
		if (strcmp(argv[i], "-d") == 0)
			delete_data = 1;
		if (strcmp(argv[i],"-i") == 0)
			disable_indexing = 1;
		if (strcmp(argv[i],"-l") == 0)
		{
			enable_logging = 1;
			//look for the file path and the page interval
			strcpy(log_file_path, argv[i+1]);
			log_file = fopen(log_file_path, "a");
			if(log_file != NULL)
				fclose(log_file);
			else
			{
				fprintf(stderr, "The file %s could not be opened or created. Please check if the path is correct.\n", log_file_path);
				return 0;
			}
			if ( i+2 < argc)
			{
				if (isdigit(argv[i+2][0]))
				{
					page_interval = atoi(argv[i+2]);
					i += 2;
				}
				else
					i += 1;
			}
		}
		if (strcmp(argv[i],"-r") == 0)
		{
			//look for the page id to restart writing from
			is_restart = 1;
			if(i+1 < argc)
			{
				if (isdigit(argv[i+1][0]))
				{
					restart_page_id = atoi(argv[i+1]);
					i += 1;
				}
			}
		}
	}
}

if (enable_logging)
	fprintf(stderr, "Log file path %s, page interval %d\n", log_file_path, page_interval);
	
if (is_restart)
	fprintf(stderr, "Restart page_id: %d\n", restart_page_id);
	
if (delete_data)
{
	fprintf(stderr, "Information from the following tables of the Wikipedia database will be deleted: page, text, revision\n");
	
	printf("TRUNCATE TABLE page;\n");
	printf("TRUNCATE TABLE text;\n");
	printf("TRUNCATE TABLE revision;\n");
}

if (disable_indexing)
{
	fprintf(stderr, "Disabling keys on tables: page, text, revision\n");
	
	printf("ALTER TABLE page DISABLE KEYS;\n");
	printf("ALTER TABLE text DISABLE KEYS;\n");
	printf("ALTER TABLE revision DISABLE KEYS;\n");
}

srand((unsigned)time(NULL));

//Creating the parser
XML_Parser p = XML_ParserCreate(NULL);
if (! p) {
	fprintf(stderr, "Couldn't allocate memory for parser\n");
	exit(-1);
}

//Setting the handlers for the parser
XML_SetElementHandler(p, wp_elem_start_handler, wp_elem_end_handler);
XML_SetCharacterDataHandler(p, wp_data_handler);

//Starting the parsing
for (;;) {
	int done;
	int len;
	len = fread(file_buf, 1, FILE_BUF_SIZE, stdin);
	if (ferror(stdin)) {
		fprintf(stderr, "Read error\n");
		exit(-1);
	}
	done = feof(stdin);
	
	if (XML_Parse(p, file_buf, len, done) == XML_STATUS_ERROR) {
		fprintf(stderr, "Parse error at line %" XML_FMT_INT_MOD "u:\n%s\n",
		XML_GetCurrentLineNumber(p),
		XML_ErrorString(XML_GetErrorCode(p)));
		exit(-1);
	}
	
	if (done)
		break;
}

printf("\n\n");

free(status_stack);
XML_ParserFree(p);

if (disable_indexing)
{
	fprintf(stderr, "Enabling keys on tables: page, text, revision\n");

	printf("ALTER TABLE page ENABLE KEYS;\n");
	printf("ALTER TABLE text ENABLE KEYS;\n");
	printf("ALTER TABLE revision ENABLE KEYS;\n");
}

return 0;
}

static void buf_strcpy(char *dest, size_t len)
{
	int i;
	strncpy(dest, data_buf, len);
	dest[len-1] = 0;
	for (i=0; dest[i]!=0; i++) {
		if (dest[i] == '\'') {
		dest[i] = '`';
	} else if (dest[i] == '\\') {
		dest[i] = ' ';
		}
	}
}
