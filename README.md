oy_sql项目是用来全局修改sql,用来隔离租户id，逻辑删除。

使用方法:
### 1.引入依赖
下面版本是自己本地maven打包的版本自己可以修改
```xml
<dependency>
    <groupId>com.oy</groupId>
    <artifactId>oy_sql</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2.yml配置

```yaml
## sql
oy:
  sql:
    # 租户相关配置
    tenant:
      # 是否开启
      open: true
      # 租户字段
      tenantColumn: store_id
    # 逻辑删除相关配置
    delete:
      # 是否开启
      open: true
      # 已经逻辑删除值是1 未删除是0
      deleteValue: 1
      normalValue: 0
      # 逻辑删除列名
      deleteColumn: is_delete
  # 配置一些额外的 mapper方法
  handle:
    # 不用拼接租户id
    noTenantIds:
      #        - com.oy.springbootmybatisdemo.mapper.UserMapper.selectById
      - com.oy.springbootmybatisdemo.mapper.UserMapper.selectByName
    # 实际删除的方法
    deleteIds:
      - com.oy.springbootmybatisdemo.mapper.UserMapper.deleteById
      - com.oy.springbootmybatisdemo.mapper.UserMapper.deleteByName
    # 不需要租户的表
    noTenantTables:
      - oy_user
      - oy_test
```

### 3. 需要实现获取租户id的方法，并注入到ioc容器
用户需要实现 ITenantService 接口
```java
@Component
public class TenantServiceImpl implements ITenantService {

    @Override
    public Long getTenantId() {
        Long tenantId = (Long) ThreadUtil.getTenantId();
        return tenantId;
    }
}
```





