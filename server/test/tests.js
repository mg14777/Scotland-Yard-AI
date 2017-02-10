var PlayerServer = require('../lib/player_server.js');
var GameServer   = require('../lib/game_server.js');
var SimpleServer = require('../lib/server.js');
var net          = require('net');
var expect       = require('chai').expect;




describe('Player Server', function() {
    var player_server = null;
    beforeEach(function() {
        player_server = new PlayerServer();
        player_server.server.on('error', function(err) {});
    });

    afterEach(function() {

        player_server.close();
        player_server = null;
    });

    describe('#listen()', function() {
        it('It should be listening for connections after calling the listen method', function(done) {
            player_server.listen(8124);
            var is_listening = false;
            setTimeout(function() {
                expect(is_listening).to.be.true;
            }, 1000);
            net.createConnection(8124, function() {
                is_listening = true;
                done();
            });
        });
    });

    describe('register', function() {
        it('Should emit a register event when a player registers', function(done) {
            var event_emitted = false;
            player_server.listen(8124);

            setTimeout(function() {
                expect(event_emitted).to.be.true;
            }, 1000);

            var client = net.createConnection(8124, function() {
                client.on('error', function() {});
                var data = {type:'REGISTER'};
                client.write(JSON.stringify(data));
            });

            player_server.on('register', function() {
                event_emitted = true;
                done();
            });
        });

        it('Should have the id of the student as the data' , function(done) {
            var id = '234mc';
            player_server.listen(8124);
            var client = net.createConnection(8124, function() {
                client.on('error', function() {});
                var data = {type:'REGISTER', student_id:id};
                client.write(JSON.stringify(data));
            });

            player_server.on('register', function(client, sent_id) {
                expect(sent_id).to.be.equal(id);
                done();
            });
        });
    });

    describe('#move', function() {
        it('Should emit a move event when a player makes a move', function(done) {
            var event_emitted = false;
            player_server.listen(8124);

            setTimeout(function() {
                expect(event_emitted).to.be.true;
            }, 1000);

            var client = net.connect(8124, function() {
                client.on('error', function() {});
                var data = {type:'MOVE'};
                client.write(JSON.stringify(data));
            });

            player_server.on('move', function(player, move) {
                event_emitted = true;
                done();
            });
        });


        it('Should pass the move as an argument to the event emission', function(done) {
            var move = {
                type: 'MOVE',
                move : {
                    target: 56,
                    ticket: 'Bus',
                    colour: 'Black'
                }
            };

            player_server.listen(8124);

            var client = net.connect(8124, function() {
                client.on('error', function() {});
                client.write(JSON.stringify(move));
            });

            player_server.on('move', function(player, incoming_move) {
                expect(incoming_move.move.target).to.be.equal(move.move.target);
                expect(incoming_move.move.ticket).to.be.equal(move.move.ticket);
                expect(incoming_move.move.colour).to.be.equal(move.move.colour);
                done();
            });
        });
    });
});


var simple_server = null;
var judge         = null;
describe('SimpleServer', function() {
    beforeEach(function(done) {

        // start the server and add the judge to it
        simple_server = new SimpleServer();
        simple_server.start(8123, 8124);


        // initialise a game on the server by connecting a judge
        judge = net.createConnection(8124, function() {
            judge.on('data', function(data) {
                var message = JSON.parse(data.toString());
                if(message.type == 'INITIALISED') done();
            });
            var init_message = {type:'INITIALISE', game_id:1};
            judge.write(JSON.stringify(init_message));
        });

    });

    afterEach(function() {
        judge.destroy();
        judge         = null;
        simple_server.close();
        simple_server = null;
    });

    describe('initialised', function() {
        it('Should respond to the initialised event emitted by the game server and store the ' +
        'game_id that the judge provided', function(done) {
            expect(simple_server.gameId()).to.be.equal(1);
            done();
        });
    });

    describe('register', function() {
        it('Should remove a colour from the list of available colours. As this is the first ' +
        'registration it should remove the colour Black', function(done) {
            var client = net.createConnection(8123, function() {
                var reg = {type: 'REGISTER', student_id: 'testId'};
                client.write(JSON.stringify(reg), function() {
                    setTimeout(function() {
                        expect(simple_server.colours.indexOf('Black')).to.be.equal(-1);
                        done();
                    }, 10);
                });
            });
        });

        it('Should add the player to the game using the game_server.addPlayer() function', function(done) {
            var client = net.createConnection(8123, function() {
                var reg = {type: 'REGISTER', student_id: 'testId'};
                client.write(JSON.stringify(reg), function() {
                    setTimeout(function() {
                        var game = simple_server.game_server.games[1];
                        expect(game.players).to.include.key('Black');
                        done();
                    }, 50);
                });
            });
        });
    });


    describe('move', function() {
        it('Should handle the "move" event from the player_server and pass the information ' +
        'to the game_server using game_server.makeMove()', function(done) {

            var move = {
                type: 'MOVE',
                move : {
                    target: 56,
                    ticket: 'Bus',
                    colour: 'Black'
                }
            };


            judge.on('data', function(data) {
                var message = JSON.parse(data.toString());
                if(message.type == 'MOVE') {
                    expect(message.move.target).to.be.equal(56);
                    done();
                }
            });

            var client = net.createConnection(8123, function() {

                client.on('data', function(data) {
                    var resp = JSON.parse(data.toString());
                    if(resp.type == 'REGISTERED') {
                        client.write(JSON.stringify(move));
                    }
                });

                var reg = {type: 'REGISTER', student_id: 'testId'};
                client.write(JSON.stringify(reg) + '\n');
            });
        });
    });


});

