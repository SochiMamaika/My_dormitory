#include "WashMachineService.h"
// Конструктор
WashMachineService::WashMachineService(const drogon::orm::DbClientPtr& dbClient)
{
    repository = std::make_shared<WashMachineRepository>(dbClient);
}

    std::list<WashMachine> WashMachineService::getWashMachines()
{
    return repository->getWashMachines();
}

void WashMachineService::addWashMachine(const std::string name,
                                        int user_id)
{
    repository->addWashMachine(name,
                               user_id);
}

bool WashMachineService::deleteWashMachine(int id)
{
    return repository->deleteWashMachine(id);
}