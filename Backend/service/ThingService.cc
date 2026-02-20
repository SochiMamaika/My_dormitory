#include "ThingService.h"
// Конструктор
ThingService::ThingService(const drogon::orm::DbClientPtr& dbClient)
    {
        repository = std::make_shared<ThingRepository>(dbClient);
    }
Thing ThingService::createThing(std::string type,
                                std::string body,
                                int room,
                                std::list<std::string> file_path,
                                int user_id)
{
    return repository->createThing(type, 
                                   body,
                                   room,
                                   file_path,
                                   user_id);
}


bool ThingService::deleteThing(int id_thing)
{
    return repository->deleteThing(id_thing);
}

std::list<Thing> ThingService::getThings()
{
    return repository->getThings();
}