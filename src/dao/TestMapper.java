//Added to test Bonus 1

package dao;

import java.util.List;

import annotations.CreateTable;
import annotations.Delete;
import annotations.MappedClass;
import annotations.Param;
import annotations.Save;
import annotations.Select;
import entity.Student;
import entity.Test;

@MappedClass(clazz=Test.class)
public interface TestMapper extends BasicMapper<Test>  // all mappers should extend BasicMapper with the correct type
{

	@CreateTable
	public void createTable();	
	
	@Save
	public void save(Test t);	
	
	@Delete
	public void delete(Test t);	
	
		
	//note: always double check SQL that it works
	//		always check if the thing being inserted is supposed to be a string, don't forget quotes
		
	@Select("select * from :table where id = :id")   
	public Test getById(@Param("id") Integer id);
	
	
	@Select("select * from :table")
	public List<Test> getAll();
	
}

