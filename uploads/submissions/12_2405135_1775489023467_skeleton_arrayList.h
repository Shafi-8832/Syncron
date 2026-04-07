#include <stdio.h>
#include <stdlib.h>

typedef struct 
{
    int *array;
    int size;
    int capacity;
    int current_position;
    // declare variables you need
} arrayList;

void delete_at_index(arrayList* list, int i);

void init(arrayList* list)
{
    list->size = 0;
    list->current_position = -1; // ***
    list->capacity = 2;
    list->array = (int*)malloc(sizeof(int) * list->capacity);
    // implement initialization
}

void free_list(arrayList* list)
{
    free(list->array);
    init(list);
    // implement destruction of list
}

void increase_capacity(arrayList* list) // intelligent and responsive
{
    if (list->size > list->capacity / 2) {
        list->capacity *= 2;
        printf("Capacity increased from %d to %d\n", list->capacity/2, list->capacity);
        list->array = (int*)realloc(list->array, sizeof(int) * list->capacity);
    }
    // implement capacity increase
}

void decrease_capacity(arrayList* list) // intelligent and responsive 
{
    if (list->size < list->capacity/4 && list->capacity > 2) {
        list->capacity /= 2;
        printf("Capacity decreased from %d to %d\n", list->capacity*2, list->capacity);
        list->array = (int*)realloc(list->array, sizeof(int) * list->capacity);
    }
    // implement capacity decrease
}

void print(arrayList* list)
{
    // implement list printing
    printf("[ ");
    if (list->size == 0) {
        printf(". ]\n");
        return;
    }
    for (int i=0; i<list->size; i++) {
        printf("%d", list->array[i]);
        if (i == list->current_position) printf("|");
        printf(" ");
    }
    printf("]\n");
}

void insert(int item, arrayList* list) // **********
{
    // implement insert function

    // Safety
    if (list->size == 0) {
        list->array[0] = item;
        list->current_position = list->size++;
        increase_capacity(list);
        return;
    }

    list->size++;
    increase_capacity(list); // if needed

    list->current_position++;

    int i = list->size-1; // last element point
    while (i != list->current_position) {
        list->array[i] = list->array[i-1];
        i--;
    }

    // insert new element
    list->array[i] = item;
}

int delete_cur(arrayList* list) // current_position doesn't move
{
    // implement deletion of element at current index position
    if (list->size == 0) return -1;

    int ret = list->array[list->current_position];

    list->size--;
    
    int i = list->current_position;
    while (i != list->size) {
        list->array[i] = list->array[i+1];
        i++;
    }

    decrease_capacity(list);
    
    if (list->current_position == list->size) list->current_position--;
    return ret;
}

void append(int item, arrayList* list)
{
    // implement append function
    if (list->size == 0) list->current_position++;

    list->array[list->size++] = item;

    increase_capacity(list);
}

int size(arrayList* list)
{
    return list->size;
    // implement size function
}

void prev(int n, arrayList* list)
{
    list->current_position -= n;
    if (list->current_position < 0) list->current_position = 0;
    // implement prev function
}

void next(int n, arrayList* list)
{
    list->current_position += n;
    if (list->current_position > list->size - 1) list->current_position = list->size - 1;
    // implement next function
}

int is_present(int n, arrayList* list)
{
    for (int i=0; i<list->size; i++) {
        if (list->array[i] == n) return 1;
    }
    return 0;
    // implement presence checking function
}

void clear(arrayList* list)
{
    // implement list clearing function
    free_list(list);
}

// Ask A Question
int delete_item(int item, arrayList* list)
{
    // implement item deletion function
    for (int i=0; i<list->size; i++) {
        if (list->array[i] == item) {
            if (i == list->current_position) delete_cur(list);
            else delete_at_index(list, i);
            // return item;
            return 1;
        }
    }
    // return -1;
    return 0;
}

void swap_ind(int ind1, int ind2, arrayList* list)
{
    // implement swap function at metioned index position
    if (ind1 < 0 || ind2 < 0 || ind1 == ind2 || ind1 >= list->size || ind2 >= list->size) {
        printf("Index out of range\n");
        return;
    }

    list->array[ind1] = list->array[ind1] ^ list->array[ind2];
    list->array[ind2] = list->array[ind1] ^ list->array[ind2];
    list->array[ind1] = list->array[ind1] ^ list->array[ind2];
}

int search(int item, arrayList* list)
{
    // implement search function
    for (int i=0; i<list->size; i++)
        if (list->array[i] == item) {
            list->current_position = i;
            return i;
        }
    return -1;
}

int find(int ind, arrayList* list)
{
    // implement find function
    if (ind >= list->size || ind < 0) return -1;
    list->current_position = ind;
    return list->array[ind];
}

int update(int ind, int value, arrayList* list)
{
    // implement update function at metioned index position

    // Error Handling?
    if (ind >= list->size || ind < 0) return -1;
    // Error Handling?
    list->current_position = ind;
    int ret = list->array[ind];
    list->array[ind] = value;
    return ret;
}

int trim(arrayList* list) // **
{
    // implement trim function
    if (list->size == 0) return -1;

    list->size--;
    decrease_capacity(list);
    
    // CURRENT POSITION CHANGING
    if (list->current_position == list->size) list->current_position--;
    
    return list->array[list->size];
}

void reverse(arrayList* list)
{
    // implement reverse function
    for (int i=0; i<list->size/2; i++) {
        int temp = list->array[i];
        list->array[i] = list->array[list->size-i-1];
        list->array[list->size-i-1] = temp;
    }
}

// you can define helper functions you need

void delete_at_index(arrayList* list, int i) {
    list->size--;
    
    list->current_position = i; // changing
    
    while (i != list->size) {
        list->array[i] = list->array[i+1];
        i++;
    }

    decrease_capacity(list);
}