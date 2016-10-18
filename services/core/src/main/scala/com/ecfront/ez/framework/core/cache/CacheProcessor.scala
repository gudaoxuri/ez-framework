package com.ecfront.ez.framework.core.cache

import com.ecfront.ez.framework.core.logger.Logging

trait CacheProcessor[T] extends Logging {

  /**
    * 暴露redis client ，用于自定义操作
    *
    * @return redis client
    */
  def client(): T

  /**
    * key是否存在
    *
    * @param key key
    * @return 是否存在
    */
  def exists(key: String): Boolean

  /**
    * 获取字符串值
    *
    * @param key key
    * @return 值
    */
  def get(key: String): String

  /**
    * 设置字符串
    *
    * @param key    key
    * @param value  value
    * @param expire 过期时间(seconds)，0表示永不过期
    */
  def set(key: String, value: String, expire: Int = 0): Unit

  /**
    * 删除key
    *
    * @param key key
    */
  def del(key: String): Unit

  /**
    * 设置列表
    *
    * @param key    key
    * @param values values
    * @param expire 过期时间(seconds)，0表示永不过期
    */
  def lmset(key: String, values: List[String], expire: Int = 0): Unit

  /**
    * 添加列表值
    *
    * @param key   key
    * @param value value
    */
  def lpush(key: String, value: String): Unit

  /**
    * 修改列表中索引对应的值
    *
    * @param key   key
    * @param value value
    * @param index 索引
    */
  def lset(key: String, value: String, index: Long): Unit

  /**
    * 弹出栈顶的列表值
    * 注意，Redis的列表是栈结构，先进后出
    *
    * @param key key
    * @return 栈顶的列表值
    */
  def lpop(key: String): String

  /**
    * 获取列表中索引对应的值
    * 注意，Redis的列表是栈结构，先进后出
    *
    * @param key   key
    * @param index 索引
    * @return 索引对应的值
    */
  def lindex(key: String, index: Int): String

  /**
    * 获取列表值的长度
    *
    * @param key key
    * @return 长度
    */
  def llen(key: String): Long

  /**
    * 获取列表中的所有值
    *
    * @param key key
    * @return 值列表
    */
  def lget(key: String): List[String]

  /**
    * 设置Hash集合
    *
    * @param key    key
    * @param values values
    * @param expire 过期时间(seconds)，0表示永不过期
    */
  def hmset(key: String, values: Map[String, String], expire: Int = 0): Unit

  /**
    * 修改Hash集合field对应的值
    *
    * @param key   key
    * @param field field
    * @param value value
    */
  def hset(key: String, field: String, value: String): Unit

  /**
    * 获取Hash集合field对应的值
    *
    * @param key   key
    * @param field field
    * @return field对应的值
    */
  def hget(key: String, field: String): String

  /**
    * 判断Hash集合field是否存在
    *
    * @param key   key
    * @param field field
    * @return 是否存在
    */
  def hexists(key: String, field: String): Boolean

  /**
    * 获取Hash集合的所有值
    *
    * @param key key
    * @return 所有值
    */
  def hgetAll(key: String): Map[String, String]

  /**
    * 删除Hash集合是对应的field
    *
    * @param key   key
    * @param field field
    */
  def hdel(key: String, field: String): Unit

  /**
    * 原子加操作
    *
    * @param key       key，key不存在时会自动创建值为0的对象
    * @param incrValue 要增加的值，必须是Long Int Float 或 Double
    * @return 操作后的值
    */
  def incr(key: String, incrValue: Long = 1): Long

  /**
    * 原子减操作
    *
    * @param key       key key，key不存在时会自动创建值为0的对象
    * @param decrValue 要减少的值，必须是Long  或 Int
    * @return 操作后的值
    */
  def decr(key: String, decrValue: Long = 1): Long

  def expire(key: String, expire: Int = 0): Unit

  def flushdb(): Unit

}