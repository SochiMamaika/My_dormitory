#pragma once
#include <iostream>
#include <list>
#include <drogon.h>

class Repair 
{
    int id;
    std::string type;
    std::string body;
    std::string date;
    int room;
    std::list<std::string> repair_paths;
    int user_id;
    bool activity;
    int repairman_id;
    bool ending;

    public:
        void fromDb(const drogon::orm::Row& result)
        {
            id = result["id"].as<int>();
            type = result["type"].as<std::string>();
            body = result["body"].as<std::string>();
            date = result["date"].as<std::string>();
            room = result["room"].as<int>();
            user_id = result["user_id"].as<int>();
            activity = result["activity"].as<bool>();
            repairman_id = result["repairman_id"].as<int>();
            ending = result["ending"].as<bool>();
        }

        // Setters
        void setId(const int& id_news)
        {
            this->id = id_news;
        }

        void setType(const std::string& type_thing)
        {
            this->type = type_thing;
        }

        void setBody(const std::string& body_thing)
        {
            this->body = body_thing;
        }

        void setDate(const std::string& date_thing)
        {
            this->date = date_thing;
        }

        void setRoom(const int& room_student)
        {
            this->room = room_student;
        }

        void setRepairPaths(const std::list<std::string>& paths)
        {
            repair_paths = paths;
        }

        void setUserId(int userId)
        {
            user_id = userId;
        }

        void setActivity(const bool& newActivity) 
        { 
            this->activity = newActivity; 
        }

        // Getters
        int getId() const
        {
            return id;
        }

        std::string getType() const
        {
            return type;
        }

        std::string getBody() const
        {
            return body;
        }

        std::string getDate() const
        {
            return date;
        }

        int getRoom() const
        {
            return room;
        }

        int getUserId() const
        {
            return user_id;
        }

        int getRepairmanId() const
        {
            return repairman_id;
        }

        std::list<std::string>& getRepairPaths()
        {
            return repair_paths;
        }

        bool getActivity()
        {
            return activity;
        }

        bool getEnding()
        {
            return ending;
        }
};