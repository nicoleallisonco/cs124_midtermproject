package orm;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import annotations.Column;
import annotations.Entity;
import annotations.MappedClass;
import dao.BasicMapper;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;

public class MyORM 
{	
	
	HashMap<Class, Class> entityToMapperMap = new HashMap<Class, Class>();
	
	
	public void init() throws Exception
	{
		// scan all mappers -- @MappedClass
		scanMappers();		
		
		// scan all the entities -- @Entity
		scanEntities();
				
		// create all entity tables
		createTables();

	}


	private void scanMappers() throws ClassNotFoundException 
	{
		// use FastClasspathScanner to scan the dao package for @MappedClass
		ScanResult results = new FastClasspathScanner("dao").scan();	
		List<String> allResults = results.getNamesOfClassesWithAnnotation(MappedClass.class);
		
		for (String s: allResults) {
			Class c = Class.forName(s);
			MappedClass mc = (MappedClass) c.getAnnotation(MappedClass.class);
			Class mcClass = mc.clazz();
			// check if the clazz has the @Entity annotation
			if (mcClass.isAnnotationPresent(Entity.class)) {
				// map the clazz to the mapper class
				entityToMapperMap.put(mcClass, c);
			}
			else {
				// if not throw new RuntimeException("No @Entity")
				throw new RuntimeException("No @Entity");
			}
		}
	}
	

	private void scanEntities() throws ClassNotFoundException 
	{
		// use FastClasspathScanner to scan the entity package for @Entity
		ScanResult results = new FastClasspathScanner("entity").scan();	
		List<String> allResults = results.getNamesOfClassesWithAnnotation(Entity.class);
		
		// go through each of the fields 
		for (String s : allResults) {
			Class c = Class.forName(s);
			Field[] fields = c.getDeclaredFields();
			// check if there is only 1 field with a Column id attribute
			int count = 0;
			for (Field f : fields)
			{
				if (f.isAnnotationPresent(Column.class))
				{
					Column column = f.getAnnotation(Column.class);
					if(column.id()==true) {
						count++;
					}
				}
			}
			if (count>1) {
				// if more than one field has id throw new RuntimeException("duplicate id=true")
				throw new RuntimeException("duplicate id=true");
			}
		}
	}
	
	
	public Object getMapper(Class clazz)
	{
		// create the proxy object for the mapper class supplied in clazz parameter
		// all proxies will use the supplied DaoInvocationHandler as the InvocationHandler
		Object object =  Proxy.newProxyInstance(
				ClassLoader.getSystemClassLoader(), 					
				new Class[] { clazz }, 
				new DaoInvocationHandler()							
				);
		
		return object;
	}
	

	private void createTables()
	{
		// go through all the Mapper classes in the map
		for (Class c : entityToMapperMap.keySet()) {
			// create a proxy instance for each
			// all these proxies can be casted to BasicMapper
			Object object = getMapper(entityToMapperMap.get(c));
			BasicMapper basicMapper = (BasicMapper) object;
			// run the createTable() method on each of the proxies
			basicMapper.createTable();
		}
	}

	

	
	
}
