package object mau {
  type Id = String
  type Key = String
  type KeyMethod[A] = Function1[A, List[Key]]
}
