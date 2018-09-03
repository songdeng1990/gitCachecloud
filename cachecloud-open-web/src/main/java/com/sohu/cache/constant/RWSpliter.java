/**
 * 
 */
package com.sohu.cache.constant;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * redis读写分离命令
 * @author 邓松
 * 
 */
public class RWSpliter {
	
	private static Set<String> writeCommands;
	private static Set<String> readCommands;

	static {
		String[] all_command = { "PING", "SET", "GET", "QUIT", "EXISTS", "DEL",
				"TYPE", "FLUSHDB", "KEYS", "RANDOMKEY", "RENAME", "RENAMENX",
				"RENAMEX", "DBSIZE", "EXPIRE", "EXPIREAT", "TTL", "SELECT",
				"MOVE", "FLUSHALL", "GETSET", "MGET", "SETNX", "SETEX", "MSET",
				"MSETNX", "DECRBY", "DECR", "INCRBY", "INCR", "APPEND",
				"SUBSTR", "HSET", "HGET", "HSETNX", "HMSET", "HMGET",
				"HINCRBY", "HEXISTS", "HDEL", "HLEN", "HKEYS", "HVALS",
				"HGETALL", "RPUSH", "LPUSH", "LLEN", "LRANGE", "LTRIM",
				"LINDEX", "LSET", "LREM", "LPOP", "RPOP", "RPOPLPUSH", "SADD",
				"SMEMBERS", "SREM", "SPOP", "SMOVE", "SCARD", "SISMEMBER",
				"SINTER", "SINTERSTORE", "SUNION", "SUNIONSTORE", "SDIFF",
				"SDIFFSTORE", "SRANDMEMBER", "ZADD", "ZRANGE", "ZREM",
				"ZINCRBY", "ZRANK", "ZREVRANK", "ZREVRANGE", "ZCARD", "ZSCORE",
				"MULTI", "DISCARD", "EXEC", "WATCH", "UNWATCH", "SORT",
				"BLPOP", "BRPOP", "AUTH", "SUBSCRIBE", "PUBLISH",
				"UNSUBSCRIBE", "PSUBSCRIBE", "PUNSUBSCRIBE", "PUBSUB",
				"ZCOUNT", "ZRANGEBYSCORE", "ZREVRANGEBYSCORE",
				"ZREMRANGEBYRANK", "ZREMRANGEBYSCORE", "ZUNIONSTORE",
				"ZINTERSTORE", "SAVE", "BGSAVE", "BGREWRITEAOF", "LASTSAVE",
				"SHUTDOWN", "INFO", "MONITOR", "SLAVEOF", "CONFIG", "STRLEN",
				"SYNC", "LPUSHX", "PERSIST", "RPUSHX", "ECHO", "LINSERT",
				"DEBUG", "BRPOPLPUSH", "SETBIT", "GETBIT", "SETRANGE",
				"GETRANGE", "EVAL", "EVALSHA", "SCRIPT", "SLOWLOG", "OBJECT",
				"BITCOUNT", "BITOP", "SENTINEL", "DUMP", "RESTORE", "PEXPIRE",
				"PEXPIREAT", "PTTL", "INCRBYFLOAT", "PSETEX", "CLIENT", "TIME",
				"MIGRATE", "HINCRBYFLOAT", "SCAN", "HSCAN", "SSCAN", "ZSCAN",
				"WAIT", "CLUSTER", "ASKING" };

		String[] writeCommandArray = { "SET", "DEL", "FLUSHDB", "RENAME",
				"RENAMENX", "RENAMEX", "EXPIRE", "EXPIREAT", "MOVE",
				"FLUSHALL", "GETSET", "SETNX", "SETEX", "MSET", "MSETNX",
				"DECRBY", "DECR", "INCRBY", "INCR", "APPEND", "HSET", "HSETNX",
				"HMSET", "HINCRBY", "HDEL", "RPUSH", "LPUSH", "LTRIM", "LSET",
				"LREM", "LPOP", "RPOP", "RPOPLPUSH", "SADD", "SREM", "SPOP",
				"SMOVE", "SINTERSTORE", "SUNIONSTORE", "MULTI", "DISCARD",
				"EXEC", "WATCH", "UNWATCH", "BLPOP", "BRPOP", "ZUNIONSTORE",
				"ZINTERSTORE", "LPUSHX", "PERSIST", "RPUSHX", "LINSERT",
				"BRPOPLPUSH", "SETBIT", "SETRANGE", "BITOP", "RESTORE",
				"PEXPIRE", "PEXPIREAT", "INCRBYFLOAT", "PSETEX",
				"HINCRBYFLOAT", "SDIFFSTORE", "ZADD", "ZREM", "ZINCRBY" };
		String[] readCommandArray = { "PING", "GET", "EXISTS", "TYPE", "KEYS",
				"RANDOMKEY", "DBSIZE", "TTL", "MGET", "SUBSTR", "HGET",
				"HMGET", "HEXISTS", "HLEN", "HKEYS", "HVALS", "HGETALL",
				"LLEN", "LRANGE", "LINDEX", "SMEMBERS", "SCARD", "SISMEMBER",
				"SINTER", "SUNION", "SORT", "ZCOUNT", "ZRANGEBYSCORE",
				"ZREVRANGEBYSCORE", "ZREMRANGEBYRANK", "ZREMRANGEBYSCORE",
				"INFO", "STRLEN", "ECHO", "GETBIT", "GETRANGE", "BITCOUNT",
				"DUMP", "PTTL", "TIME", "SCAN", "HSCAN", "SSCAN", "ZSCAN",
				"SDIFF", "SRANDMEMBER", "ZRANGE", "ZRANK", "ZREVRANK",
				"ZREVRANGE", "ZCARD", "ZSCORE" };

		writeCommands = new HashSet();
		readCommands = new HashSet();
		for (int i = 0; i < writeCommandArray.length; i++) {
			writeCommands.add(writeCommandArray[i]);			
		}
		for (int i = 0; i < readCommandArray.length; i++) {
			readCommands.add(readCommandArray[i]);
		}
	}

	public static boolean isWriteCommand(String commandName) {
		commandName = commandName.toUpperCase();	
		return writeCommands.contains(commandName);
	}

}
