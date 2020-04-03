package orm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import annotations.Column;
import annotations.CreateTable;
import annotations.Delete;
import annotations.Entity;
import annotations.MappedClass;
import annotations.Save;
import annotations.Select;
import realdb.GhettoJdbcBlackBox;

public class DaoInvocationHandler implements InvocationHandler {

	static GhettoJdbcBlackBox jdbc;
	
	public DaoInvocationHandler() {
		// TODO Auto-generated constructor stub
		
		if (jdbc==null)
		{
			jdbc = new GhettoJdbcBlackBox();
			jdbc.init("com.mysql.cj.jdbc.Driver", 				// DO NOT CHANGE
					  "jdbc:mysql://localhost/jdbcblackbox?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC",    // change jdbcblackbox to the DB name you wish to use
					  "root", 									// USER NAME
					  "");										// PASSWORD
		}
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		// determine method annotation type and call the appropriate method
			// @CreateTable
			// @Save
			// @Delete
			// @Select
		
		if(method.isAnnotationPresent(CreateTable.class)) {
			
			createTable(method);
			
		} else if(method.isAnnotationPresent(Save.class)) {
			
			save(method, args);
			
		} else if(method.isAnnotationPresent(Delete.class)) {
			
			delete(method, args);
			
		} else if(method.isAnnotationPresent(Select.class)) {
			
			select(method, args);
			
		}
			
		return null;
	}
	
	
	// HELPER METHOD: when putting in field values into SQL, strings are in quotes otherwise they go in as is
	private String getValueAsSql(Object o) throws Exception
	{
		if (o.getClass()==String.class)
		{
			return "\""+o+"\"";
		}
		else
		{
			return String.valueOf(o);
		}		
	}
	
	
	// handles @CreateTable
	private void createTable(Method method)
	{
		
// 		SAMPLE SQL 		
//	    CREATE TABLE REGISTRATION (id INTEGER not NULL AUTO_INCREMENT,
//												first VARCHAR(255), 
//												last VARCHAR(255), age INTEGER, PRIMARY KEY ( id ))
		
		String sqlStatement = "CREATE TABLE ";
		String idStatement = "";
		
// 		Using the @MappedClass annotation from method
		// get the required class 		
		Class c = method.getDeclaringClass().getAnnotation(MappedClass.class).clazz();
		
		//Add the table name by using the table attribute in the entity class
		sqlStatement = sqlStatement + ((Entity) c.getAnnotation(Entity.class)).table().toUpperCase() + " (";
		
		// use reflection to check all the fields for @Column
		// use the @Column attributed to generate the required sql statment
		Field[] fields = c.getDeclaredFields();
		
		int numFields = fields.length;
		int counter = 0;
		
		for(Field f : fields) {
			
			f.setAccessible(true);
			if(f.isAnnotationPresent(Column.class)) {
				
				//String name = f.getName();
				//Get attributes of Column
				String name = f.getAnnotation(Column.class).name();
				String sqlType = f.getAnnotation(Column.class).sqlType();
				Boolean id = f.getAnnotation(Column.class).id();
				
				sqlStatement = sqlStatement + name + " " + sqlType;
				
				//Check if we should add a comma (shouldn't add if its the last)
				if(counter < numFields - 1) {
					sqlStatement += ", ";
				}
				
				//This determines which field is the primary id 
				if(id) {
					idStatement = idStatement + ", PRIMARY KEY ( " + name + " ))";
				}
				
			}
			counter++;
		}
		
		//Check which closing statement should be taken 
		if(idStatement.equals("")) {
			sqlStatement += ")";
		} else {
			sqlStatement = sqlStatement + idStatement;
		}
		
// 		Run the sql
		jdbc.runSQL(sqlStatement);
	}
	
	// handles @Delete
	private void delete(Method method, Object o) throws Exception
	{
// 		SAMPLE SQL		
//  	DELETE FROM REGISTRATION WHERE ID=1
		
		
// 		Using the @MappedClass annotation from method
		// get the required class 		
		// use reflection to check all the fields for @Column
		// find which field is the primary key
		// for the Object o parameter, get the value of the field and use this as the primary value 
		// for the WHERE clause
				// if the primary key field value is null, throw a RuntimeException("no pk value")
		
		String sqlStatement = "DELETE FROM ";
		String idStatement = "";
		
		Class c = method.getDeclaringClass().getAnnotation(MappedClass.class).clazz();
		Field[] fields = c.getDeclaredFields();
		Field[] oFields = o.getClass().getDeclaredFields();
		
		//Add the table type by using the class name
		sqlStatement = sqlStatement + ((Entity) c.getAnnotation(Entity.class)).table() + " ";
		
		int counter = 0;
		int pkId = -1;
		
		for(Field f:fields) {
			
			f.setAccessible(true);
			if(f.isAnnotationPresent(Column.class)) {
				
				Boolean id = f.getAnnotation(Column.class).id();
				
				//This determines which field is the primary id 
				if(id) {
					pkId = counter;
				}
				
			}
			counter++;
		}
		
		//Getting the getId method through reflection (using object o)
		String valMethod = "get" + oFields[pkId].getName().substring(0, 1).toUpperCase() 
			      + oFields[pkId].getName().substring(1) + "()";
		
		Method m = o.getClass().getDeclaredMethod(valMethod);
		
		try {
			
			if(m.invoke(o) != null) {	
				
				String fieldPk = fields[pkId].getAnnotation(Column.class).name();
				sqlStatement = sqlStatement + "WHERE " + fieldPk + "=" + m.invoke(o).toString();
				
			}
			
		} catch (RuntimeException e) {
			System.out.println("no pk value");
		}
		
		// run the sql
		jdbc.runSQL(sqlStatement);
	}
	
	// handles @Save
	private void save(Method method, Object o) throws Exception
	{
// 		Using the @MappedClass annotation from method
		// get the required class 		
		// use reflection to check all the fields for @Column
		// find which field is the primary key
		// for the Object o parameter, get the value of the field
			// if the field is null run the insert(Object o, Class entityClass, String tableName) method
			// if the field is not null run the update(Object o, Class entityClass, String tableName) method
		
		Class c = method.getDeclaringClass().getAnnotation(MappedClass.class).clazz();
		Field[] fields = c.getDeclaredFields();
		
		System.out.println(o.toString());
		
		Class objectClass = Class.forName(o.toString());
		Field[] oFields = objectClass.getDeclaredFields();
		
		System.out.println(oFields.length);
		
		int counter = 0;
		int pkId = -1;
		
		for(Field f:fields) {
			
			f.setAccessible(true);
			if(f.isAnnotationPresent(Column.class)) {
				
				Boolean id = f.getAnnotation(Column.class).id();
				//This determines which field is the primary id 
				if(id) {
					pkId = counter;
				}
				
			}
			counter++;
		}
		
		String valMethod = "get" + oFields[pkId].getName().substring(0, 1).toUpperCase() 
			      + oFields[pkId].getName().substring(1) + "()";
		
		Method m = o.getClass().getDeclaredMethod(valMethod);
		
		if(m.invoke(o) == null) {		
			insert(o, c, ((Entity) c.getAnnotation(Entity.class)).table());		
		} else {		
			update(o, c, ((Entity) c.getAnnotation(Entity.class)).table());
		}
		
	}

	private void insert(Object o, Class entityClass, String tableName) throws Exception 
	{
		
		
// 		SAMPLE SQL		
//		INSERT INTO table_name (column1, column2, column3, ...)
//		VALUES (value1, value2, value3, ...)	


//		HINT: columnX comes from the entityClass, valueX comes from o 
		String sqlStatement = "INSERT INTO " + tableName + " (";
		String valStatement = "VALUES (";
		
		Field[] fields = entityClass.getDeclaredFields();
		Field[] oFields = o.getClass().getDeclaredFields();
		
		int numFields = fields.length;
		int counter = 0;
		
		for(Field f:fields) {
			
			f.setAccessible(true);
			if(f.isAnnotationPresent(Column.class)) {
				
				String name = f.getAnnotation(Column.class).name();
				String valMethod = "get" + oFields[counter].getName().substring(0, 1).toUpperCase() 
						      + oFields[counter].getName().substring(1) + "()";
				
				Method method = o.getClass().getDeclaredMethod(valMethod);
				
				sqlStatement = sqlStatement + name;
				valStatement = valStatement + method.invoke(o);
				
				//Check if we should add a comma (shouldn't add if its the last)
				if(counter < numFields - 1) {
					sqlStatement += ", ";
					valStatement += ", ";
				}
				
			}
			counter++;
		}

		sqlStatement = sqlStatement +") "+ valStatement + ") ";
		
// 		Run the sql
		jdbc.runSQL(sqlStatement);
	}

	private void update(Object o, Class entityClass, String tableName) throws IllegalAccessException, Exception {

//		SAMPLE SQL		
//		UPDATE table_name
//		SET column1 = value1, column2 = value2, ...
//		WHERE condition;
		
//		HINT: columnX comes from the entityClass, valueX comes from o 		
		
		String sqlStatement = "UPDATE " + tableName + " SET";
		String whereStatement = "WHERE ";
		
		Field[] fields = entityClass.getDeclaredFields();
		Field[] oFields = o.getClass().getDeclaredFields();
		
		int numFields = fields.length;
		int counter = 0;
		int pkId = -1;
		
		for(Field f:fields) {
			
			f.setAccessible(true);
			if(f.isAnnotationPresent(Column.class)) {
				
				String name = f.getAnnotation(Column.class).name();
				String valMethod = "get" + oFields[counter].getName().substring(0, 1).toUpperCase() 
						      + oFields[counter].getName().substring(1) + "()";
				
				Method method = o.getClass().getDeclaredMethod(valMethod);
				
				sqlStatement = sqlStatement + name + " = " + method.invoke(o);
				
				//Check if we should add a comma (shouldn't add if its the last)
				if(counter < numFields - 1) {
					sqlStatement += ", ";
				}
				
				Boolean id = f.getAnnotation(Column.class).id();
				
				//This determines which field is the primary id 
				if(id) {
					whereStatement = whereStatement + name + " = " + method.invoke(o);
				}
				
			}
			counter++;
		}

		sqlStatement = sqlStatement + whereStatement;
		
// 		Run the sql
		jdbc.runSQL(sqlStatement);
		
//		run sql
//		jdbc.runSQL(SQL STRING);
	}

		
	// handles @Select
	private Object select(Method method, Object[] args) throws Exception
	{
		// same style as lab
		
// PART I		
// 		Using the @MappedClass annotation from method
//		get the required class
//		Use this class to extra all the column information (this is the replacement for @Results/@Result)		
//		generate the SELECT QUERY		

// PART II
		
//		this will pull actual values from the DB		
//		List<HashMap<String, Object>> results = jdbc.runSQLQuery(SQL QUERY);

		
		// process list based on getReturnType
		if (method.getReturnType()==List.class)
		{
			List returnValue = new ArrayList();
			
			// create an instance for each entry in results based on mapped class
			// map the values to the corresponding fields in the object
			// DO NOT HARD CODE THE TYPE and FIELDS USE REFLECTION
			
			return returnValue;
		}
		else
		{
			// if not a list return type
			
			// if the results.size() == 0 return null
			// if the results.size() >1 throw new RuntimeException("More than one object matches")
			// if the results.size() == 1
				// create one instance based on mapped class
				// map the values to the corresponding fields in the object
				// DO NOT HARD CODE THE TYPE and FIELDS USE REFLECTION
						
			return null;
		}
	}
	
}
