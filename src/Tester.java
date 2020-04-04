import dao.StudentMapper;
import dao.SubjectMapper;
import dao.TestMapper;
import entity.Student;
import entity.Subject;
import entity.Test;
import orm.MyORM;

public class Tester {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		MyORM orm = new MyORM();
		orm.init();
		
		StudentMapper sm = (StudentMapper) orm.getMapper(StudentMapper.class);
		
		for (int i = 0; i<10; i++)
		{
			Student s = new Student();
			//s.setId(i);
			s.setAge(10+i);
			s.setFirst("Test"+i);
			s.setLast("Test"+i);
			sm.save(s);
		}
		
		System.out.println(sm.getById(4));
		
		System.out.println(sm.getAll());
		
		System.out.println(sm.getByFirstNameAndLastName("Test1", "Test1"));
		
		SubjectMapper sbm = (SubjectMapper) orm.getMapper(SubjectMapper.class);
		
		Subject sb = new Subject();
		sb.setName("cs124");
		sb.setNumStudents(20);
		
		sbm.save(sb);
		
		//Added to test Bonus 1
		
		TestMapper tm = (TestMapper) orm.getMapper(TestMapper.class);
		
		Test t = new Test();
		t.setName("Bonus test");
		
		tm.save(t);
		
		Test t2 = new Test();
		t2.setName("Bonus test2");
		
		tm.save(t2);
		
		Test tr = tm.getById(2);
		tr.setName("Updated");
		
		tm.save(tr);
		
		System.out.println(tm.getById(1));
		
		System.out.println(tm.getAll());
		
		tm.delete(tm.getById(1));
	}

}
