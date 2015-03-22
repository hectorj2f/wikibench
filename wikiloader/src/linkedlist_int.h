//	Copyright (c) 2007, Guido Urdaneta  (guidox@gmail.com)
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

#ifndef _GUDS_LINKED_LIST_int
#define _GUDS_LINKED_LIST_int

#include <stdio.h>

typedef struct _GULLNodeInt GULLNodeInt;
typedef struct _GULinkedListInt GULinkedListInt;



struct _GULLNodeInt
{
    int data;
    GULLNodeInt *next;
    GULLNodeInt *prev;
};

struct _GULinkedListInt
{
    GULLNodeInt *head;
    GULLNodeInt *tail;
    int size;
    int (*cmp)(const int , const int );
    
    void (*destroy)(GULinkedListInt *list);
    void (*destroy_apply)(GULinkedListInt *list, void (*pfunc)(int ));
    void (*add)(GULinkedListInt *list, int data);
    void (*add_front)(GULinkedListInt *list, int data);
    int (*contains)(GULinkedListInt *list, int data);
    int (*remove)(GULinkedListInt *list, int data);
    int (*remove_front)(GULinkedListInt *list, int *rem);
    int (*remove_end)(GULinkedListInt *list, int *rem);
    int (*insert_after)(GULinkedListInt *list, GULLNodeInt *node, int data);
    int (*insert_before)(GULinkedListInt *list, GULLNodeInt *node, int data);
    int (*remove_node)(GULinkedListInt *list, GULLNodeInt *node);
    void (*clear)(GULinkedListInt *list);
    GULinkedListInt *(*clone)(GULinkedListInt *list);
    void (*apply_ctx)(GULinkedListInt *list, void *ctx, void (*pfunc)(void *ctx, int ));
    void (*apply)(GULinkedListInt *list, void (*pfunc)(int ));
    void (*print)(GULinkedListInt *list, const char *fmt);
    void (*fprint)(GULinkedListInt *list, FILE *f, const char *fmt);
    void (*fprint_ind)(GULinkedListInt *list, FILE *f, int ind, const char *fmt);
    void (*fprintgen)(GULinkedListInt *list, FILE *f, void (*pfunc)(FILE *,int ));
    void (*fprintgen_ind)(GULinkedListInt *list, FILE *f, int ind, void (*pfunc)(FILE *,int ));
};

GULinkedListInt *gull_int_new(int (*cmp)(const int , const int ));
void gull_int_destroy(GULinkedListInt *list);
void gull_int_destroy_apply(GULinkedListInt *list, void (*pfunc)(int ));
void gull_int_add(GULinkedListInt *list, int data);
void gull_int_add_front(GULinkedListInt *list, int data);
int gull_int_contains(GULinkedListInt *list, int data);
int gull_int_remove(GULinkedListInt *list, int data);
int gull_int_remove_front(GULinkedListInt *list, int *rem);
int gull_int_remove_end(GULinkedListInt *list, int *rem);
int gull_int_insert_after(GULinkedListInt *list, GULLNodeInt *node, int data);
int gull_int_insert_before(GULinkedListInt *list, GULLNodeInt *node, int data);
int gull_int_remove_node(GULinkedListInt *list, GULLNodeInt *node);
void gull_int_clear(GULinkedListInt *list);
GULinkedListInt *gull_int_clone(GULinkedListInt *list);
void gull_int_apply_ctx(GULinkedListInt *list, void *ctx, void (*pfunc)(void *ctx, int ));
void gull_int_apply(GULinkedListInt *list, void (*pfunc)(int ));
void gull_int_print(GULinkedListInt *list, const char *fmt);
void gull_int_fprint(GULinkedListInt *list, FILE *f, const char *fmt);
void gull_int_fprint_ind(GULinkedListInt *list, FILE *f, int ind, const char *fmt);
void gull_int_fprintgen(GULinkedListInt *list, FILE *f, void (*pfunc)(FILE *,int ));
void gull_int_fprintgen_ind(GULinkedListInt *list, FILE *f, int ind, void (*pfunc)(FILE *,int ));


#endif
