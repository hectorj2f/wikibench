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

#include <stdio.h>
#include <stdlib.h>
#include "linkedlist_int.h"


#define EQUAL(a,b) ((list->cmp == NULL) ? ((a)==(b)) : (!((*list->cmp)((a),(b)))))
#define gull_front(list) ((list)->head->data)
#define gull_end(list) ((list)->end->data)

//#define INDENT(f,i,n) for ((i)=0;((i)<(n));((i)++)) fprintf((f),"\t")
#define INDENT(f,n) {int _k_; for ((_k_)=0;((_k_)<(n));((_k_)++)) fprintf((f),"\t");}

/**
 * Creates a node that can be destroyed with free.
 * data field must be handled by user.
 */
static GULLNodeInt *gull_int_new_node(void)
{
    GULLNodeInt *ret = (GULLNodeInt *) malloc(sizeof(GULLNodeInt));
    ret->next = NULL;
    ret->prev = NULL;
    return ret;
}

GULinkedListInt *gull_int_new(int (*cmp)(const int , const int ))
{
    GULinkedListInt *ret = (GULinkedListInt *) malloc(sizeof(struct _GULinkedListInt));
    ret->head = NULL;
    ret->tail = NULL;
    ret->size = 0;
    ret->cmp = cmp;

    ret->destroy = gull_int_destroy;
    ret->destroy_apply = gull_int_destroy_apply;
    ret->add = gull_int_add;
    ret->add_front = gull_int_add_front;
    ret->contains = gull_int_contains;
    ret->remove = gull_int_remove;
    ret->remove_front = gull_int_remove_front;
    ret->remove_end = gull_int_remove_end;
    ret->insert_after = gull_int_insert_after;
    ret->insert_before = gull_int_insert_before;
    ret->remove_node = gull_int_remove_node;
    ret->clear = gull_int_clear;
    ret->clone = gull_int_clone;
    ret->apply_ctx = gull_int_apply_ctx;
    ret->apply = gull_int_apply;
    ret->print = gull_int_print;
    ret->fprint = gull_int_fprint;
    ret->fprint_ind = gull_int_fprint_ind;
    ret->fprintgen = gull_int_fprintgen;
    ret->fprintgen_ind = gull_int_fprintgen_ind;

    return ret;
}

void gull_int_destroy(GULinkedListInt *list)
{
    GULLNodeInt *cur=NULL;

    if (list == NULL)
        return;
    
    cur = list->head;

    while (cur != NULL) {
        GULLNodeInt *tmp = cur->next;
        free(cur);
        cur = tmp;
    }

    free(list);
}

void gull_int_destroy_apply(GULinkedListInt *list, void (*pfunc)(int ))
{
    GULLNodeInt *cur=NULL;

    if (list == NULL)
        return;

    cur = list->head;

    while (cur != NULL) {
        GULLNodeInt *tmp = cur->next;
        (*pfunc)(cur->data);
        free(cur);
        cur = tmp;
    }

    free(list);
}

void gull_int_add(GULinkedListInt *list, int data)
{
    GULLNodeInt *node = gull_int_new_node();
    node->data = data;
    node->next = NULL;
    node->prev=list->tail;
    if (list->tail != NULL)
        list->tail->next = node;
    list->tail = node;
    if (list->head == NULL)
        list->head = node;
    list->size++;
}

void gull_int_add_front(GULinkedListInt *list, int data)
{
    GULLNodeInt *node = gull_int_new_node();
    node->data = data;
    node->next = list->head;
    node->prev = NULL;
    if (list->head != NULL)
        list->head->prev = node;
    list->head = node;
    if (list->tail == NULL)
        list->tail = node;
    list->size++;
}

int gull_int_contains(GULinkedListInt *list, int data)
{
    GULLNodeInt *i;
    for (i=list->head; i!=NULL; i=i->next)
        if (EQUAL(i->data, data))
            return 1;

    return 0;
}

int gull_int_remove(GULinkedListInt *list, int data)
{
    GULLNodeInt *i;
    for (i=list->head; i!=NULL; i=i->next)
        if (EQUAL(i->data, data)) {
            GULLNodeInt *prev = i->prev;
            GULLNodeInt *next = i->next;
            if (prev != NULL)
                prev->next = next;
            else
                list->head = next;
            if (next != NULL)
                next->prev = prev;
            else
                list->tail = prev;

            free(i);
            list->size--;
            return 1;
        }

    return 0;
}

int gull_int_remove_front(GULinkedListInt *list, int *rem)
{
    GULLNodeInt *h = list->head;
    if (h == NULL) return -1;
    *rem = h->data;
    if (h == list->tail) {
        list->tail = NULL;
        list->head = NULL;
    } else {
        list->head = h->next;
        list->head->prev = NULL;
    }
    list->size--;
    free(h);

    return 0;
}

int gull_int_remove_end(GULinkedListInt *list, int *rem)
{
    GULLNodeInt *t = list->tail;
    if (t == NULL) return -1;
    *rem = t->data;
    if (t == list->head) {
        list->head = NULL;
        list->tail = NULL;
    } else {
        list->tail = t->prev;
        list->tail->next = NULL;
    }
    list->size--;
    free(t);

    return 0;
}

int gull_int_insert_after(GULinkedListInt *list, GULLNodeInt *node, int data)
{
    GULLNodeInt *new_node;
    if (node == NULL) return -1;
    new_node = gull_int_new_node();
    new_node->data = data;
    new_node->next = node->next;
    if (new_node->next != NULL) {
        new_node->next->prev = new_node;
    }
    new_node->prev = node;
    node->next = new_node;
    if (list->tail == node) {
        list->tail = new_node;
    }
    list->size++;
    
    return 0;
}

int gull_int_insert_before(GULinkedListInt *list, GULLNodeInt *node, int data)
{
    GULLNodeInt *new_node;
    if (node == NULL) return -1;
    new_node = gull_int_new_node();
    new_node->data = data;
    new_node->prev = node->prev;
    if (new_node->prev != NULL) {
        new_node->prev->next = new_node;
    }
    new_node->next = node;
    node->prev = new_node;
    if (list->head == node) {
        list->head = new_node;
    }
    list->size++;
    
    return 0;
}

int gull_int_remove_node(GULinkedListInt *list, GULLNodeInt *node)
{
    if (node == NULL) return -1;
    if (node->next != NULL) {
        node->next->prev = node->prev;
    }
    if (node->prev !=NULL) {
        node->prev->next = node->next;
    }
    if (list->head == node) {
        list->head = node->next;
    }
    if (list->tail == node) {
        list->tail = node->prev;
    }

    list->size--;
    free(node);

    return 0;
}


void gull_int_clear(GULinkedListInt *list)
{
    GULLNodeInt *cur=list->head;

    while (cur != NULL) {
        GULLNodeInt *tmp = cur->next;
        free(cur);
        cur = tmp;
    }
    list->head = NULL;
    list->tail = NULL;
    list->size = 0;
}

GULinkedListInt *gull_int_clone(GULinkedListInt *list)
{
    GULinkedListInt *clone = gull_int_new(list->cmp);
    GULLNodeInt *i;
    for (i=list->head; i!=NULL; i=i->next)
        gull_int_add(clone, i->data);

    return clone;
}

void gull_int_apply_ctx(GULinkedListInt *list, void *ctx, void (*pfunc)(void *ctx, int ))
{
    GULLNodeInt *i;
    for (i=list->head; i!=NULL; i=i->next) {
        (*pfunc)(ctx, i->data);
    }
}

void gull_int_apply(GULinkedListInt *list, void (*pfunc)(int ))
{
    GULLNodeInt *i;
    for (i=list->head; i!=NULL; i=i->next) {
        (*pfunc)(i->data);
    }
}


void gull_int_print(GULinkedListInt *list, const char *fmt)
{
    gull_int_fprint(list, stdout, fmt);
}

void gull_int_fprint(GULinkedListInt *list, FILE *f, const char *fmt)
{
    GULLNodeInt *i;
    fprintf(f, "{ ");
    for (i=list->head; i!=NULL; i=i->next)
    {
        fprintf(f,fmt, i->data);
        if (i->next != NULL) fprintf(f, ", ");
    }
    fprintf(f, " }");
}

void gull_int_fprint_ind(GULinkedListInt *list, FILE *f, int ind, const char *fmt)
{
    INDENT(f,ind);
    gull_int_fprint(list, f, fmt);
    fprintf(f, "\n");
}

void gull_int_fprintgen(GULinkedListInt *list, FILE *f, void (*pfunc)(FILE *,int ))
{
    GULLNodeInt *i;
    fprintf(f, "{ ");
    for (i=list->head; i!=NULL; i=i->next)
    {
        (*pfunc)(f,i->data);
        if (i->next != NULL) fprintf(f, ", ");
    }
    fprintf(f, " }");
}

void gull_int_fprintgen_ind(GULinkedListInt *list, FILE *f, int ind, void (*pfunc)(FILE *,int ))
{
    GULLNodeInt *i;
    INDENT(f,ind);
    fprintf(f, "linked_list {\n");
    for (i=list->head; i!=NULL; i=i->next)
    {
        INDENT(f,ind+1);
        (*pfunc)(f, i->data);
        fprintf(f, "\n");
    }
    INDENT(f,ind);
    fprintf(f, " }\n");
}

