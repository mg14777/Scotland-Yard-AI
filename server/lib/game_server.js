'use strict'
var net = require('net');
var events = require('events');
var util = require('util');


function GameServer() {
    var self = this;
    this.debug = false;
    this.games = [];
    this.sockets = [];

    this.server = net.createServer(function(judge) {

        judge.on('data', function(data) {
            var parts = data.toString().split('\n');
            for(var i = 0; i < parts.length; i++) {
                if(parts[i].length > 0) self.parseInput(judge, parts[i]);
            }
        });
    });
}
util.inherits(GameServer, events.EventEmitter);

/**
 * Function to parse the input data
 * @param judge
 * @param data
 */
GameServer.prototype.parseInput = function(judge, data) {
    var message = JSON.parse(data);

    // if the judge is joining then we create a new game
    if(message.type == 'INITIALISE') {
        this.initialiseGame(judge, message);
    }
    else if(message.type == 'NOTIFY_TURN') {
        var player = this.getPlayerFromJudge(judge, message.colour);
        this.notifyTurn(player, message);
    }
    else if(message.type == 'NOTIFY') {
        var spectators = this.getSpectatorsFromJudge(judge);
        for(var id in spectators) {
            this.notifyMove(spectators[id], message);
        }
    }
    else if(message.type == 'READY') {
        var spectators = this.getSpectatorsFromJudge(judge);
        for(var id in spectators) {
            spectators[id].write(JSON.stringify(message) + '\n')
        }
    }
    else if(message.type == 'GAME_OVER') {
        var game = this.games[this.sockets[this.socketId(judge)]];
        var spectators = this.getSpectatorsFromJudge(judge);
        for(var id in spectators) {
            spectators[id].write(JSON.stringify(message) + '\n')
        }

        this.emit('game_over', game.game_id);
    }
}






/**
 * Notify a single player of a move
 * @param player
 * @param move
 */
GameServer.prototype.notifyMove = function(player, move) {
    player.write(JSON.stringify(move) + '\n');
}


/**
 * Function to get a certain player asssociated to a judge
 * given the players colour
 * @param judge
 * @param colour
 * @returns {*}
 */
GameServer.prototype.getPlayerFromJudge = function(judge, colour) {
    var game = this.getGameBySocketId(this.socketId(judge));
    return game.players[colour];
}



GameServer.prototype.getSpectatorsFromJudge = function(judge, colour) {
    var game = this.getGameBySocketId(this.socketId(judge));
    return game.spectators;
}


/**
 *
 * Function to get a list of players associated with a judge
 * @param judge
 * @returns {Array}
 */
GameServer.prototype.getPlayersFromJudge = function(judge) {
    return this.getGameBySocketId(this.socketId(judge)).players;
}


/**
 * Function to initialise a game on a request from a judge
 * It will add the judge to the list of judges, create the
 * game and then send a message to the judge to say that it
 * has been done
 * @param judge
 * @param message
 */
GameServer.prototype.initialiseGame = function(judge, message) {

    // add the judge and create the game
    this.write('judge added for game ' + message.game_id);
    this.addSocket(judge, message.game_id);
    this.createGame(judge, message.game_id);

    // send the message to the judge that this is done
    var ok_message = {type:'INITIALISED'};
    judge.write(JSON.stringify(ok_message) + '\n');


    this.emit('initialised', message.game_id);
}


/**
 * Function to notify a given player that it is their turn
 * to take a move
 * @param player
 * @param location
 */
GameServer.prototype.notifyTurn = function (player, message) {
    player.write(JSON.stringify(message) + '\n');
}


/**
 * Get the game associated with a socket id
 * @param id
 * @returns {*}
 */
GameServer.prototype.getGameBySocketId = function(id) {
    var gameId = this.sockets[id];
    if(this.hasGame(gameId))
        return this.games[gameId];
    return null;

}


/**
 * Add a socket to the list given a socket and
 * game id
 * @param judge
 * @param game_id
 */
GameServer.prototype.addSocket = function(socket, game_id) {
    var id = this.socketId(socket);
    if(!this.hasSocket(id))
        this.sockets[id] = game_id;
}


/**
 * Check that a certain socket exists
 * @param id
 * @returns {boolean}
 */
GameServer.prototype.hasSocket = function(id) {
    return (typeof this.sockets[id] !== 'undefined');
}



/**
 * Function to create a new game. Game ids are checked to avoid
 * duplicates
 * @param judge
 * @param game_id
 */
GameServer.prototype.createGame = function(judge, game_id) {

    // is this game already created
    if(this.hasGame(game_id)) return;

    var game = {};
    game.players = [];
    game.judge = judge;
    game.spectators = [];
    this.games[game_id] = game;


    this.write('game created: ' + game_id);


}


/**
 * Function to add a new player to a game
 * @param player
 * @param colour
 * @param game_id
 */
GameServer.prototype.addPlayerToGame = function(player, colour, game_id) {
    if(!this.hasGame(game_id)) return false;
    var id = this.socketId(player);
    this.sockets[id] = game_id;
    this.games[game_id].players[colour] = player;
    this.games[game_id].spectators[id] = player;

    var message = {type:'REGISTERED', colour: colour};
    player.write(JSON.stringify(message) + '\n');

    this.write('added player to game ' + game_id);
    return true;
}


GameServer.prototype.addPlayer = function(player, colour, game_id) {

    // add the player to the game
    this.addPlayerToGame(player, colour, game_id);

    // send a message to the judge of the game
    var id = this.socketId(player);
    this.sockets[id] = game_id;
    this.games[game_id].players[colour] = player;
    this.write('added player to game ' + game_id);

    var response = {type: 'JOIN', colour: colour};
    this.games[game_id].judge.write(JSON.stringify(response) + '\n');
}


/**
 * Get the id constructed from a sockets remote port
 * and remote address
 * @param client
 * @returns {string}
 */
GameServer.prototype.socketId = function(client) {
    return client.remoteAddress + ':' + client.remotePort;
}


/**
 * Function to make a move, The move is translated to json
 * and sent to the judge socket
 * @param player
 * @param move
 */
GameServer.prototype.makeMove = function(player, move) {
    var id = this.socketId(player);
    var game_id = this.sockets[id];
    this.write('move for game ' + game_id);
    this.games[game_id].judge.write(JSON.stringify(move) + '\n');
}


/**
 * Function to check if a game exists given an id
 * @param game_id
 * @returns {boolean}
 */
GameServer.prototype.hasGame = function(game_id) {
    return (typeof this.games[game_id] !== 'undefined');
}



GameServer.prototype.listen = function(port) {
    this.server.listen(port);
}


GameServer.prototype.close = function() {
    this.server.close();
}



GameServer.prototype.write = function(data) {
    if(this.debug) console.log('[Game Server  ] ' + data);
}


module.exports = GameServer;


