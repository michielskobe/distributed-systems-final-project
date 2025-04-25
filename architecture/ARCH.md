# Dapp final project architecture

## 1: Front end

A client will connect with their browser over HTTP to a spring boot server running the front end. There are a few notes about this: 

 * This will contain the store for the users
 * This will contain authentication for managers and users
 * This will contain order overviews/status
 * This will contain management features for managers

Flow: 

 * Order is placed by a user
 * This order gets stored in the SQL database and a command to process it gets put into the queue

## 2: Broker

The broker consists of an Azure Storage Account Queue and a background worker. Together they process the commands which are issued by the front end. 

 * The Queue is redundand within one datacenter to ensure no messages get lost when a server gets deleted
 * The Queue features a receive and delete architecture for the workers. This means a worker receives a message, which does NOT delete the message in the queue; instead it is hidden for 30 minutes from other workers. Only when a worker finishes its task does a message get deleted. If a worker crashes, the message re-appears for other workers after the time-out. 
 * Workers get scaled horizontally based upon the queue length. 
 * Workers are responsible for negotiating with all suppliers and figuring out if the order can be carried out
 * The worker updates the SQL database with the status of the order

## 3: Suppliers

The suppliers are essentially just API's which offer products, they can be implement using a very simple springboot application and a proper SQL database deployed on a managed azure solution. 

 * They all have the same API which includes the following endpoints for placing the order: `/reserve`, `/commit`, `/rollback`. 
 * A worker reserves an item on each supplier, if one reserve fails, everything gets canceled and the order is not possible
 * If all reserves work, then they all get commited
 * If something goes wrong during the operations, a rollback of the commit can be done. 
 * A reservation only lasts for 15 minutes, to avoid binding up products in transactions which take too long (for example due to a crashed worker)

## 4: DB

The DB is a managed DB by Azure, and provides a cost-effective way of storing information about orders.
