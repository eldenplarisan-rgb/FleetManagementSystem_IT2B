package com.scc.model;

public record User(int user_id, String name,String username, String password, String roles, String status) {
    @Override
    public String toString(){
        return String.format("[%d] %s (%s) - Status: %s", user_id,username,roles,status);

    }
}