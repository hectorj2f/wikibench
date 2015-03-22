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

#ifndef _WIKILOADER_H
#define _WIKILOADER_H

#define TITLE_LEN 260
#define USERNAME_LEN 260
#define COMMENT_LEN 4000
#define TEXT_LEN 10000
#define FILENAME_LEN 260
#define SRC_LEN 400
#define TIMESTAMP_LEN 15

/* Structures for parsing the XML Dumps and temporarily storing the data*/
struct _Page
{
  int id;
  char title[TITLE_LEN];
  int namespace;
  char restrictions[COMMENT_LEN];
  int counter;
  int is_redirect;
  int is_new;
  double random;
  char touched[TIMESTAMP_LEN];
  int latest;
  int len; 
};
typedef struct _Page Page;

struct _Revision
{
  int id;
  char timestamp[TIMESTAMP_LEN];
  char user_text[USERNAME_LEN];
  int user;
  char comment[COMMENT_LEN];
  int page;
  int text_id;
  int minor_edit;
  int deleted;
  int len;
  int parent_id;
};
typedef struct _Revision Revision;

struct _Text
{
  int id;
  char text[TEXT_LEN];
  char flags[SRC_LEN];
};

typedef struct _Text Text;


#endif
