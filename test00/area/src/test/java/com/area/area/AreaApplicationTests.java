package com.area.area;

import com.area.area.entity.Area;
import com.area.area.mapper.AreaMapper;
import junit.framework.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class AreaApplicationTests {
	@Autowired
	private AreaMapper areaMapper;
	@Test
	void contextLoads() {
		System.out.println(("----- selectAll method test ------"));
		List<Area> userList = areaMapper.selectList(null);
		Assert.assertEquals(5, userList.size());
		userList.forEach(System.out::println);
	}

}
