/*
Copyright (c) 2020 Beever

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.example.beever.database;

import java.util.Map;

/**
 * An abstract class on which both EventEntry and TodoEntry is based.
 * This is made mostly for convenience in modifyEventOrTodo() in UserEntry.class and GroupEntry.class.
 */
public abstract class EventTodoEntry implements MapEntry {

    /**
     * Get equivalent Map object representation which obeys EventEntry/TodoEntry contract
     * @return Map object representation
     */
    public abstract Map<String, Object> retrieveRepresentation();

    /**
     * Check whether this is a group (and not personal) event/todo
     * @return boolean for whether this is a group entity
     */
    public abstract boolean checkIfGroupEntry();

    /**
     * Get user/group ID of entity containing this event/todo
     * @return user/group ID
     */
    public abstract String retrieveSource();
}
