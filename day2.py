def levels(line):
  return [int(s) for s in line.split()]

with open("src/test/resources/day2.txt") as file:
  records = [levels(line) for line in file.readlines()]

def deltas(levels):
  try:
    prev = next(levels)
  except StopIteration:
    return
  for item in levels:
    yield item - prev
    prev = item

def is_safe(deltas):
  sgn = 0
  for delta in deltas:
    absDelta = abs(delta)
    if (absDelta < 1 or absDelta > 3):
      return False
    newSgn = 1 if delta > 0 else -1
    if (sgn != 0 and newSgn != sgn):
      return False
    sgn = newSgn
  return True

def dampened(levels, index):
  return (levels[i] for i in range(len(levels)) if i != index)

def is_safe_with_dampening(levels):
  if is_safe(deltas(iter(levels))):
    return True
  return any(is_safe(deltas(dampened(levels, i))) for i in range(len(levels)))

print(sum(1 for record in records if (is_safe(deltas(iter(record))))))
print(sum(1 for record in records if (is_safe_with_dampening(record))))
