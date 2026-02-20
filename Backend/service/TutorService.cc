#include "TutorService.h"
// Конструктор
TutorService::TutorService(const drogon::orm::DbClientPtr& dbClient)
    {
        repository = std::make_shared<TutorRepository>(dbClient);
    }
Tutor TutorService::createTutor(std::string header,
                                std::string body,
                                std::list<std::string> image_path,
                                int user_id)
{
    return repository->createTutor(header,
                                   body,
                                   image_path,
                                   user_id);
}

bool TutorService::deleteTutor(int id_tutor)
{
    return repository->deleteTutor(id_tutor);
}

std::list<Tutor> TutorService::getTutor()
{
    return repository->getTutor();
}